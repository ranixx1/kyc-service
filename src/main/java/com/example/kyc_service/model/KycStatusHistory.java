package com.example.kyc_service.model;

import com.example.kyc_service.enums.SubmissionStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "kyc_status_history")
@Getter
@NoArgsConstructor
public class KycStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submission_id", nullable = false)
    private KycSubmission submission;

    @Enumerated(EnumType.STRING)
    @Column
    private SubmissionStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubmissionStatus newStatus;

    private Long changedByUserId;

    private String changedByUsername;

    @Column(nullable = false, updatable = false)
    private LocalDateTime changedAt;

    @PrePersist
    void prePersist() {
        changedAt = LocalDateTime.now();
    }

    public static KycStatusHistory system(KycSubmission submission,
                                          SubmissionStatus previous,
                                          SubmissionStatus next) {
        return build(submission, previous, next, null, null);
    }

    public static KycStatusHistory byAnalyst(KycSubmission submission,
                                             SubmissionStatus previous,
                                             SubmissionStatus next,
                                             Long analystId,
                                             String analystUsername) {
        return build(submission, previous, next, analystId, analystUsername);
    }

    private static KycStatusHistory build(KycSubmission submission,
                                          SubmissionStatus previous,
                                          SubmissionStatus next,
                                          Long userId,
                                          String username) {
        var entry = new KycStatusHistory();
        entry.submission = submission;
        entry.previousStatus = previous;
        entry.newStatus = next;
        entry.changedByUserId = userId;
        entry.changedByUsername = username;
        return entry;
    }
}