package com.example.kyc_service.dto;

import com.example.kyc_service.enums.DocumentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RegisterExpectedDataRequest(
        @NotNull Long userId,
        @NotNull DocumentType documentType,
        @NotBlank String expectedHolderName,
        @NotBlank String expectedDocumentNumber
) {}