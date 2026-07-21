package com.example.kyc_service.model;

import com.example.kyc_service.enums.SubmissionStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "kyc_status_history")
@Getter
@Setter
@NoArgsConstructor
public class KycStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submission_id", nullable = false)
    private KycSubmission submission;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private SubmissionStatus previousStatus;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private SubmissionStatus newStatus;

    // null quando a transição é feita pelo sistema (OCR automático)
    @Column
    private Long changedByUserId;

    @Column
    private String changedByUsername;

    @Column(nullable = false)
    private LocalDateTime changedAt;

    @PrePersist
    public void prePersist() {
        this.changedAt = LocalDateTime.now();
    }

    public static KycStatusHistory of(KycSubmission submission,
                                      SubmissionStatus previous,
                                      SubmissionStatus next,
                                      Long changedByUserId,
                                      String changedByUsername) {
        KycStatusHistory h = new KycStatusHistory();
        h.submission = submission;
        h.previousStatus = previous;
        h.newStatus = next;
        h.changedByUserId = changedByUserId;
        h.changedByUsername = changedByUsername;
        return h;
    }
}