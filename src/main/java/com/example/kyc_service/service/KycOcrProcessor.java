package com.example.kyc_service.service;

import com.example.kyc_service.enums.DocumentType;
import com.example.kyc_service.enums.SubmissionStatus;
import com.example.kyc_service.model.KycStatusHistory;
import com.example.kyc_service.repository.KycStatusHistoryRepository;
import com.example.kyc_service.repository.KycSubmissionRepository;
import com.example.kyc_service.service.analysis.DocumentAnalysis;
import com.example.kyc_service.service.analysis.DocumentAnalyzer;
import com.example.kyc_service.storage.MinioStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.UUID;

/**
 * Handles asynchronous document processing after upload.
 *
 * Responsibilities:
 * - Download the file from MinIO
 * - Delegate analysis to DocumentAnalyzer
 * - Persist the result and update submission status
 *
 * This class knows nothing about OCR or business rules —
 * it only coordinates I/O and persistence.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class KycOcrProcessor {

    private final KycSubmissionRepository submissionRepository;
    private final KycStatusHistoryRepository historyRepository;
    private final MinioStorageService storageService;
    private final DocumentAnalyzer documentAnalyzer;

    @Async
    public void process(UUID submissionId, String fileKey,
                        String mimeType, DocumentType documentType) {
        log.info("Processing started. submissionId={}", submissionId);
        try {
            try (InputStream stream = storageService.download(fileKey)) {
                DocumentAnalysis analysis = documentAnalyzer.analyze(stream, mimeType, documentType);
                persist(submissionId, analysis);
            }
        } catch (Exception e) {
            log.error("Processing failed. submissionId={}: {}", submissionId, e.getMessage(), e);
            persistFailure(submissionId);
        }
    }

    // ── Private ───────────────────────────────────────────────────────────────

    @Transactional
    void persist(UUID submissionId, DocumentAnalysis analysis) {
        submissionRepository.findById(submissionId).ifPresent(submission -> {
            SubmissionStatus previous = submission.getStatus();

            if (analysis.isPassed()) {
                submission.markInProgress(analysis.getRawText(), analysis.getConfidenceScore());
            } else {
                submission.markManualReview(analysis.getRawText(), analysis.getConfidenceScore());
            }

            submissionRepository.save(submission);
            historyRepository.save(
                    KycStatusHistory.system(submission, previous, submission.getStatus()));

            log.info("Submission updated. id={}, status={}, summary={}",
                    submissionId, submission.getStatus(), analysis.getSummary());
        });
    }

    @Transactional
    void persistFailure(UUID submissionId) {
        submissionRepository.findById(submissionId).ifPresent(submission -> {
            SubmissionStatus previous = submission.getStatus();
            submission.markManualReview(null, 0.0);
            submissionRepository.save(submission);
            historyRepository.save(
                    KycStatusHistory.system(submission, previous, SubmissionStatus.MANUAL));
        });
    }
}