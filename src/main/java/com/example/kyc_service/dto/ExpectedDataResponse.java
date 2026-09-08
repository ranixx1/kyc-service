package com.example.kyc_service.dto;

import com.example.kyc_service.enums.DocumentType;
import com.example.kyc_service.model.KycExpectedData;

import java.time.LocalDateTime;
import java.util.UUID;

public record ExpectedDataResponse(
        UUID id,
        Long userId,
        DocumentType documentType,
        String expectedHolderName,
        String expectedDocumentNumber,
        boolean consumed,
        UUID linkedSubmissionId,
        String registeredByAnalystUsername,
        LocalDateTime createdAt,
        LocalDateTime consumedAt
) {
    public static ExpectedDataResponse from(KycExpectedData e) {
        return new ExpectedDataResponse(
                e.getId(),
                e.getUserId(),
                e.getDocumentType(),
                e.getExpectedHolderName(),
                e.getExpectedDocumentNumber(),
                e.isConsumed(),
                e.getLinkedSubmissionId(),
                e.getRegisteredByAnalystUsername(),
                e.getCreatedAt(),
                e.getConsumedAt()
        );
    }
}