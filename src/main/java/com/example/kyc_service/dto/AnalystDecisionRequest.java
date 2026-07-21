package com.example.kyc_service.dto;

import com.example.kyc_service.enums.RejectionReason;
import jakarta.validation.constraints.NotBlank;

public record AnalystDecisionRequest(
        @NotBlank String action,
        RejectionReason rejectionReason,
        String note
) {}