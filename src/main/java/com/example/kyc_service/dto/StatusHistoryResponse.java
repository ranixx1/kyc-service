package com.example.kyc_service.dto;

import com.example.kyc_service.enums.SubmissionStatus;
import com.example.kyc_service.model.KycStatusHistory;

import java.time.LocalDateTime;

public record StatusHistoryResponse(
        SubmissionStatus previousStatus,
        SubmissionStatus newStatus,
        String changedByUsername,
        LocalDateTime changedAt
) {
    public static StatusHistoryResponse from(KycStatusHistory h) {
        return new StatusHistoryResponse(
                h.getPreviousStatus(),
                h.getNewStatus(),
                h.getChangedByUsername() != null ? h.getChangedByUsername() : "System (OCR)",
                h.getChangedAt()
        );
    }
}