package com.example.kyc_service.model;

import com.example.kyc_service.enums.DocumentType;
import com.example.kyc_service.enums.RejectionReason;
import com.example.kyc_service.enums.SubmissionStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "kyc_submissions")
@Getter
@Setter
@NoArgsConstructor
public class KycSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    // ID do usuário vindo do JWT
    @Column(nullable = false)
    private Long userId;

    // Username vindo do JWT — para exibição na listagem do analista
    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private DocumentType documentType;

    // Chave do objeto no MinIO (ex: "submissions/uuid/rg.jpg")
    @Column(nullable = false)
    private String fileKey;

    // image/jpeg ou application/pdf
    @Column(nullable = false)
    private String fileMimeType;

    // Tamanho do arquivo em bytes
    @Column(nullable = false)
    private Long fileSizeBytes;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private SubmissionStatus status = SubmissionStatus.NEW;

    // Resultado bruto do OCR — texto extraído (truncado em 2000 chars)
    @Column(columnDefinition = "TEXT")
    private String ocrRawText;

    // Score de confiança do OCR (0.0 a 1.0)
    @Column
    private Double ocrConfidenceScore;

    // ID do analista que tomou a decisão (claim userId do JWT)
    @Column
    private Long analystId;

    // Username do analista — para exibição
    @Column
    private String analystUsername;

    @Column
    @Enumerated(EnumType.STRING)
    private RejectionReason rejectionReason;

    // Observação livre do analista (opcional)
    @Column(columnDefinition = "TEXT")
    private String analystNote;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // Quantas vezes este usuário reenviou (útil para detectar abuso)
    @Column(nullable = false)
    private Integer resubmissionCount = 0;

    @OneToMany(mappedBy = "submission", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("changedAt ASC")
    private List<KycStatusHistory> history = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ── Factory method ────────────────────────────────────────────────────────

    public static KycSubmission create(Long userId, String username, DocumentType documentType,
                                       String fileKey, String fileMimeType, Long fileSizeBytes) {
        KycSubmission s = new KycSubmission();
        s.userId = userId;
        s.username = username;
        s.documentType = documentType;
        s.fileKey = fileKey;
        s.fileMimeType = fileMimeType;
        s.fileSizeBytes = fileSizeBytes;
        s.status = SubmissionStatus.IN_PROGRESS;
        return s;
    }

    // ── Transições de status ──────────────────────────────────────────────────
    public void markManualReview(String ocrText, double confidence) {
        this.ocrRawText = ocrText != null && ocrText.length() > 2000
                ? ocrText.substring(0, 2000)
                : ocrText;
        this.ocrConfidenceScore = confidence;
        this.status = SubmissionStatus.MANUAL;
    }

    public void approve(Long analystId, String analystUsername, String note) {
        this.analystId = analystId;
        this.analystUsername = analystUsername;
        this.analystNote = note;
        this.status = SubmissionStatus.APPROVED;
    }

    public void reject(Long analystId, String analystUsername,
                       RejectionReason reason, String note) {
        this.analystId = analystId;
        this.analystUsername = analystUsername;
        this.rejectionReason = reason;
        this.analystNote = note;
        this.status = SubmissionStatus.REJECTED;
    }
}