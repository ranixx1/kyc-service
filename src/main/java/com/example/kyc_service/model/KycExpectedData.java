package com.example.kyc_service.model;

import com.example.kyc_service.enums.DocumentType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Data pre-registered by an analyst/admin BEFORE the client submits a document.
 *
 * Example: an analyst knows in advance that user #42 was asked to submit an
 * ID_CARD for "JOAO DA SILVA", document number "12.345.678-9". When that
 * submission arrives and passes OCR/validation/fraud checks cleanly, the
 * pipeline compares the extracted holder name and document number against
 * this record and can APPROVE or REJECT automatically — no analyst needed
 * for that specific decision.
 *
 * One record is consumed by (at most) one submission. If it doesn't match,
 * it is still marked consumed — the analyst must register a new expectation
 * for a retry rather than let the same one be checked against forever.
 */
@Entity
@Table(name = "kyc_expected_data")
@Getter
@NoArgsConstructor
public class KycExpectedData {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentType documentType;

    @Column(nullable = false)
    private String expectedHolderName;

    @Column(nullable = false)
    private String expectedDocumentNumber;

    @Column(nullable = false)
    private Long registeredByAnalystId;

    @Column(nullable = false)
    private String registeredByAnalystUsername;

    @Column(nullable = false)
    private boolean consumed = false;

    private UUID linkedSubmissionId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime consumedAt;

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
    }

    public static KycExpectedData create(Long userId, DocumentType documentType,
            String expectedHolderName, String expectedDocumentNumber,
            Long analystId, String analystUsername) {
        var e = new KycExpectedData();
        e.userId = userId;
        e.documentType = documentType;
        e.expectedHolderName = expectedHolderName;
        e.expectedDocumentNumber = expectedDocumentNumber;
        e.registeredByAnalystId = analystId;
        e.registeredByAnalystUsername = analystUsername;
        return e;
    }

    public void markConsumed(UUID submissionId) {
        this.consumed = true;
        this.linkedSubmissionId = submissionId;
        this.consumedAt = LocalDateTime.now();
    }
}