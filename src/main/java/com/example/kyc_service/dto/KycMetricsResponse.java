package com.example.kyc_service.dto;

import java.util.Map;

public record KycMetricsResponse(
        long total,
        long pending,
        long inProgress,
        long manualReview,
        long approved,
        long rejected,
        Map<String, Long> byDocumentType
) {}