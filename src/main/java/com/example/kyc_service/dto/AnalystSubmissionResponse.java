package com.example.kyc_service.dto;

import com.example.kyc_service.enums.DocumentType;
import com.example.kyc_service.enums.RejectionReason;
import com.example.kyc_service.enums.SubmissionStatus;
import com.example.kyc_service.model.KycSubmission;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public record AnalystSubmissionResponse(
        UUID id,
        Long userId,
        String username,
        DocumentType documentType,
        String fileMimeType,
        Long fileSizeBytes,
        SubmissionStatus status,
        Double ocrConfidenceScore,
        String ocrRawText,
        Map<String, String> extractedFields,
        Long analystId,
        String analystUsername,
        RejectionReason rejectionReason,
        String analystNote,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static AnalystSubmissionResponse from(KycSubmission s) {
        return new AnalystSubmissionResponse(
                s.getId(),
                s.getUserId(),
                s.getUsername(),
                s.getDocumentType(),
                s.getFileMimeType(),
                s.getFileSizeBytes(),
                s.getStatus(),
                s.getOcrConfidenceScore(),
                s.getOcrRawText(),
                s.getExtractedFields(),
                s.getAnalystId(),
                s.getAnalystUsername(),
                s.getRejectionReason(),
                s.getAnalystNote(),
                s.getCreatedAt(),
                s.getUpdatedAt()
        );
    }
}