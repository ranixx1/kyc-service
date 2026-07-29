package com.example.kyc_service.service;

import com.example.kyc_service.dto.*;
import com.example.kyc_service.enums.DocumentType;
import com.example.kyc_service.enums.SubmissionStatus;
import com.example.kyc_service.exception.KycBusinessException;
import com.example.kyc_service.exception.SubmissionNotFoundException;
import com.example.kyc_service.model.KycStatusHistory;
import com.example.kyc_service.model.KycSubmission;
import com.example.kyc_service.repository.KycStatusHistoryRepository;
import com.example.kyc_service.repository.KycSubmissionRepository;
import com.example.kyc_service.storage.MinioStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class KycSubmissionService {

    private static final List<String> ALLOWED_MIME_TYPES = List.of(
            "image/jpeg", "image/jpg", "image/png", "application/pdf"
    );
    private static final long MAX_FILE_SIZE_BYTES = 10L * 1024 * 1024;

    // Statuses that indicate a submission is still active — blocks a new one for the same document type
    private static final List<SubmissionStatus> ACTIVE_STATUSES = List.of(
            SubmissionStatus.NEW,
            SubmissionStatus.IN_PROGRESS,
            SubmissionStatus.MANUAL
    );

    private final KycSubmissionRepository submissionRepository;
    private final KycStatusHistoryRepository historyRepository;
    private final MinioStorageService storageService;
    private final KycOcrProcessor ocrProcessor;

    // ── Client ────────────────────────────────────────────────────────────────

    @Transactional
    public SubmissionResponse submit(MultipartFile file, DocumentType documentType,
                                     Long userId, String username) {
        validateFile(file);
        validateNoActiveSubmission(userId, documentType);

        String fileKey = storageService.upload(file, userId);
        KycSubmission submission = KycSubmission.create(
                userId, username, documentType, fileKey, file.getContentType(), file.getSize());
        KycSubmission saved = submissionRepository.save(submission);

        historyRepository.save(KycStatusHistory.system(saved, null, SubmissionStatus.NEW));
        ocrProcessor.process(saved.getId(), fileKey, file.getContentType(), documentType);

        log.info("Submission created. id={}, userId={}, documentType={}", saved.getId(), userId, documentType);
        return SubmissionResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<SubmissionResponse> listMySubmissions(Long userId) {
        return submissionRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(SubmissionResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public SubmissionResponse getMySubmission(UUID id, Long userId) {
        return submissionRepository.findByIdAndUserId(id, userId)
                .map(SubmissionResponse::from)
                .orElseThrow(() -> new SubmissionNotFoundException(id));
    }

    // ── Analyst ───────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<AnalystSubmissionResponse> listForAnalyst(SubmissionStatus status,
                                                          DocumentType documentType,
                                                          String username,
                                                          Pageable pageable) {
        return submissionRepository
                .findWithFilters(status, documentType, username, pageable)
                .map(AnalystSubmissionResponse::from);
    }

    @Transactional(readOnly = true)
    public AnalystSubmissionResponse getForAnalyst(UUID id) {
        return submissionRepository.findById(id)
                .map(AnalystSubmissionResponse::from)
                .orElseThrow(() -> new SubmissionNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public String generateDocumentUrl(UUID id) {
        KycSubmission submission = submissionRepository.findById(id)
                .orElseThrow(() -> new SubmissionNotFoundException(id));
        return storageService.generatePresignedUrl(submission.getFileKey());
    }

    @Transactional
    public AnalystSubmissionResponse processDecision(UUID id, AnalystDecisionRequest request,
                                                     Long analystId, String analystUsername) {
        KycSubmission submission = submissionRepository.findById(id)
                .orElseThrow(() -> new SubmissionNotFoundException(id));

        if (!submission.isDecidable()) {
            throw new KycBusinessException(
                    "Decision not allowed for submission with status: " + submission.getStatus());
        }

        SubmissionStatus previous = submission.getStatus();
        applyDecision(submission, request, analystId, analystUsername);

        KycSubmission updated = submissionRepository.save(submission);
        historyRepository.save(
                KycStatusHistory.byAnalyst(updated, previous, updated.getStatus(), analystId, analystUsername));

        log.info("Decision recorded. id={}, action={}, analyst={}", id, request.action(), analystUsername);
        return AnalystSubmissionResponse.from(updated);
    }

    @Transactional(readOnly = true)
    public List<StatusHistoryResponse> getHistory(UUID id) {
        return historyRepository.findBySubmissionIdOrderByChangedAtAsc(id)
                .stream()
                .map(StatusHistoryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public KycMetricsResponse getMetrics() {
        long total = submissionRepository.count();

        Map<SubmissionStatus, Long> byStatus = submissionRepository.countGroupedByStatus()
                .stream()
                .collect(Collectors.toMap(
                        row -> (SubmissionStatus) row[0],
                        row -> (Long) row[1],
                        Long::sum,
                        () -> new EnumMap<>(SubmissionStatus.class)
                ));

        Map<String, Long> byType = submissionRepository.countByDocumentType()
                .stream()
                .collect(Collectors.toMap(
                        row -> row[0].toString(),
                        row -> (Long) row[1]
                ));

        return new KycMetricsResponse(
                total,
                byStatus.getOrDefault(SubmissionStatus.NEW, 0L),
                byStatus.getOrDefault(SubmissionStatus.IN_PROGRESS, 0L),
                byStatus.getOrDefault(SubmissionStatus.MANUAL, 0L),
                byStatus.getOrDefault(SubmissionStatus.APPROVED, 0L),
                byStatus.getOrDefault(SubmissionStatus.REJECTED, 0L),
                byType
        );
    }

    // ── Private ───────────────────────────────────────────────────────────────

    private void applyDecision(KycSubmission submission, AnalystDecisionRequest request,
                                Long analystId, String analystUsername) {
        switch (request.action().toUpperCase()) {
            case "APPROVE" ->
                    submission.approve(analystId, analystUsername, request.note());
            case "REJECT" -> {
                if (request.rejectionReason() == null) {
                    throw new KycBusinessException("Rejection reason is required.");
                }
                submission.reject(analystId, analystUsername, request.rejectionReason(), request.note());
            }
            default -> throw new KycBusinessException("Invalid action: " + request.action());
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new KycBusinessException("File cannot be empty.");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new KycBusinessException("File exceeds the maximum allowed size of 10 MB.");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase())) {
            throw new KycBusinessException("File type not allowed. Please upload JPG, PNG or PDF.");
        }
    }

    private void validateNoActiveSubmission(Long userId, DocumentType documentType) {
        boolean hasActive = submissionRepository
                .existsByUserIdAndDocumentTypeAndStatusIn(userId, documentType, ACTIVE_STATUSES);
        if (hasActive) {
            throw new KycBusinessException(
                    "An active submission already exists for document type: " + documentType.name() +
                    ". Wait for it to be resolved before submitting again.");
        }
    }
}