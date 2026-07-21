package com.example.kyc_service.dto;
import java.util.Map;

public record KycMetricsResponse(
        long totalSubmissions,
        long pendings,
        long in_progress,
        long manual,
        long approveds,
        long rejecteds,
        Map<String, Long> TipOfDocument
) {}