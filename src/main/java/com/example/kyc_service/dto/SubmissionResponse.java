package com.example.kyc_service.dto;

import com.example.kyc_service.enums.DocumentType;
import com.example.kyc_service.enums.RejectionReason;
import com.example.kyc_service.enums.SubmissionStatus;
import com.example.kyc_service.model.KycSubmission;

import java.time.LocalDateTime;
import java.util.UUID;

public record SubmissionResponse(
        UUID id,
        DocumentType documentType,
        SubmissionStatus status,
        RejectionReason rejectionReason,
        String analystNote,
        Long fileSizeBytes,
        Integer resubmissionCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static SubmissionResponse from(KycSubmission s) {
        return new SubmissionResponse(
                s.getId(),
                s.getDocumentType(),
                s.getStatus(),
                s.getRejectionReason(),
                s.getAnalystNote(),
                s.getFileSizeBytes(),
                s.getResubmissionCount(),
                s.getCreatedAt(),
                s.getUpdatedAt()
        );
    }
}