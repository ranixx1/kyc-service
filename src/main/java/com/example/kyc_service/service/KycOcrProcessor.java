package com.example.kyc_service.service;

import com.example.kyc_service.enums.DocumentType;
import com.example.kyc_service.enums.RejectionReason;
import com.example.kyc_service.enums.SubmissionStatus;
import com.example.kyc_service.model.KycExpectedData;
import com.example.kyc_service.model.KycStatusHistory;
import com.example.kyc_service.model.KycSubmission;
import com.example.kyc_service.repository.KycExpectedDataRepository;
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

@Component
@RequiredArgsConstructor
@Slf4j
public class KycOcrProcessor {

    private final KycSubmissionRepository submissionRepository;
    private final KycStatusHistoryRepository historyRepository;
    private final KycExpectedDataRepository expectedDataRepository;
    private final MinioStorageService storageService;
    private final DocumentAnalyzer documentAnalyzer;
    private final KycAutoDecisionEngine autoDecisionEngine;

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

    @Transactional
    void persist(UUID submissionId, DocumentAnalysis analysis) {
        submissionRepository.findById(submissionId).ifPresent(submission -> {
            SubmissionStatus previous = submission.getStatus();

            if (analysis.isPassed()) {
                submission.markInProgress(analysis.getRawText(), analysis.getConfidenceScore());
            } else {
                submission.markManualReview(analysis.getRawText(), analysis.getConfidenceScore());
            }

            if (analysis.getExtractedFields() != null && !analysis.getExtractedFields().isEmpty()) {
                submission.applyExtractedFields(analysis.getExtractedFields());
            }

            if (analysis.getValidationResult() != null && analysis.getValidationResult().hasErrors()) {
                submission.applyValidationErrors(analysis.getValidationResult().errors());
            }

            boolean fraudSuspicious = analysis.getFraudResult() != null && analysis.getFraudResult().suspicious();
            if (fraudSuspicious) {
                submission.applyFraudIndicators(analysis.getFraudResult().indicators());
                // A fraud signal always forces manual review, even if OCR/validation passed clean.
                // No auto-decision (below) will run on top of this.
                if (submission.getStatus() != SubmissionStatus.MANUAL) {
                    submission.markManualReview(analysis.getRawText(), analysis.getConfidenceScore());
                }
            }

            submissionRepository.save(submission);

            // Record the OCR/validation/fraud-driven transition BEFORE running
            // auto-decision, so this entry reflects only what the pipeline itself
            // did (e.g. NEW -> IN_PROGRESS). If auto-decision then moves the
            // submission further (-> APPROVED/REJECTED), that's recorded as its
            // own separate history entry below, with its own actor label.
            historyRepository.save(
                    KycStatusHistory.system(submission, previous, submission.getStatus()));

            applyAutoDecisionIfEligible(submission, analysis);

            log.info("Submission updated. id={}, status={}, fieldsExtracted={}, validationErrors={}, " +
                            "fraudSuspicious={}, summary={}",
                    submissionId,
                    submission.getStatus(),
                    analysis.getExtractedFields() != null ? analysis.getExtractedFields().size() : 0,
                    analysis.getValidationResult() != null ? analysis.getValidationResult().errorCount() : 0,
                    fraudSuspicious,
                    analysis.getSummary());
        });
    }

    /**
     * Runs the auto-decision engine and, if eligible, applies APPROVE/REJECT
     * to the submission without a human analyst. No-op if the engine can't
     * decide (unsupported type, no expected data, fraud flagged, etc.) —
     * in that case the submission stays exactly as `persist()` above left it
     * (IN_PROGRESS or MANUAL), waiting for a human like before.
     */
    private void applyAutoDecisionIfEligible(KycSubmission submission, DocumentAnalysis analysis) {
        KycAutoDecisionEngine.Decision decision = autoDecisionEngine.evaluate(submission, analysis);

        if (!decision.isApprove() && !decision.isReject()) {
            log.info("No auto-decision applied. submissionId={}, outcome={}, reason={}",
                    submission.getId(), decision.outcome(), decision.reason());
            return;
        }

        SubmissionStatus beforeDecision = submission.getStatus();
        KycExpectedData expected = decision.expectedData();

        if (decision.isApprove()) {
            submission.approve(null, "SYSTEM_AUTO_DECISION", decision.reason());
        } else {
            submission.reject(null, "SYSTEM_AUTO_DECISION", RejectionReason.INCONSISTENT_DATA, decision.reason());
        }

        expected.markConsumed(submission.getId());
        expectedDataRepository.save(expected);
        submissionRepository.save(submission);

        historyRepository.save(
                KycStatusHistory.byAutoDecision(submission, beforeDecision, submission.getStatus()));

        log.info("Auto-decision applied. submissionId={}, outcome={}, newStatus={}, reason={}",
                submission.getId(), decision.outcome(), submission.getStatus(), decision.reason());
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