package com.example.kyc_service.model;

import com.example.kyc_service.enums.DocumentType;
import com.example.kyc_service.enums.RejectionReason;
import com.example.kyc_service.enums.SubmissionStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "kyc_submissions")
@Getter
@NoArgsConstructor
public class KycSubmission {

    private static final int OCR_TEXT_MAX_LENGTH = 2000;

    private static final List<SubmissionStatus> ANALYST_DECIDABLE_STATUSES = List.of(
            SubmissionStatus.MANUAL
    );

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String username;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentType documentType;

    @Column(nullable = false)
    private String fileKey;

    @Column(nullable = false)
    private String fileMimeType;

    @Column(nullable = false)
    private Long fileSizeBytes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubmissionStatus status = SubmissionStatus.NEW;

    @Column(columnDefinition = "TEXT")
    private String ocrRawText;

    private Double ocrConfidenceScore;

    private Long analystId;

    private String analystUsername;

    @Enumerated(EnumType.STRING)
    private RejectionReason rejectionReason;

    @Column(columnDefinition = "TEXT")
    private String analystNote;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private Integer resubmissionCount = 0;

    @OneToMany(mappedBy = "submission", cascade = CascadeType.ALL, orphanRemoval = true,
               fetch = FetchType.LAZY)
    @OrderBy("changedAt ASC")
    private List<KycStatusHistory> history = new ArrayList<>();

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ── Factory ───────────────────────────────────────────────────────────────

    public static KycSubmission create(Long userId, String username, DocumentType documentType,
                                       String fileKey, String fileMimeType, Long fileSizeBytes) {
        var submission = new KycSubmission();
        submission.userId = userId;
        submission.username = username;
        submission.documentType = documentType;
        submission.fileKey = fileKey;
        submission.fileMimeType = fileMimeType;
        submission.fileSizeBytes = fileSizeBytes;
        return submission;
    }

    // ── Transições de status ──────────────────────────────────────────────────

    public void markPreApproved(String ocrText, double confidence) {
        applyOcrResult(ocrText, confidence);
        status = SubmissionStatus.APPROVED;
    }

    public void markManualReview(String ocrText, double confidence) {
        applyOcrResult(ocrText, confidence);
        status = SubmissionStatus.MANUAL;
    }

    public void approve(Long analystId, String analystUsername, String note) {
        applyAnalystDecision(analystId, analystUsername, note);
        status = SubmissionStatus.APPROVED;
    }

    public void reject(Long analystId, String analystUsername, RejectionReason reason, String note) {
        applyAnalystDecision(analystId, analystUsername, note);
        rejectionReason = reason;
        status = SubmissionStatus.REJECTED;
    }

    public boolean isDecidable() {
        return ANALYST_DECIDABLE_STATUSES.contains(status);
    }

    // ── Privados ──────────────────────────────────────────────────────────────

    private void applyOcrResult(String ocrText, double confidence) {
        ocrRawText = ocrText != null && ocrText.length() > OCR_TEXT_MAX_LENGTH
                ? ocrText.substring(0, OCR_TEXT_MAX_LENGTH)
                : ocrText;
        ocrConfidenceScore = confidence;
    }

    private void applyAnalystDecision(Long id, String username, String note) {
        analystId = id;
        analystUsername = username;
        analystNote = note;
    }
}