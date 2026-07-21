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
}