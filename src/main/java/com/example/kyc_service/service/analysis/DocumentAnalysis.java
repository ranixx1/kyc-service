package com.example.kyc_service.service.analysis;

import com.example.kyc_service.enums.DocumentType;
import com.example.kyc_service.service.analysis.document.ExtractedDocument;
import com.example.kyc_service.service.analysis.validation.ValidationResult;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

/**
 * The result of a full document analysis.
 *
 * Step 1: rawText, confidenceScore, matchedPatterns, totalPatterns, passed, summary
 * Step 2: extractedDocument, extractedFields
 * Step 3: validationResult
 *
 * Future:
 * - score (int 0–100) — Step 7
 * - fraudIndicators   — Step 11
 */
@Getter
@Builder
public class DocumentAnalysis {

    private final DocumentType documentType;
    private final String rawText;
    private final double confidenceScore;
    private final int matchedPatterns;
    private final int totalPatterns;
    private final boolean passed;
    private final String summary;

    /** Typed extracted document. Null if extraction was not attempted or failed. */
    private final ExtractedDocument extractedDocument;

    /** Flattened fields for serialization and analyst panel display. */
    private final Map<String, String> extractedFields;

    /**
     * Result of the validation engine run.
     * Null if validation was not attempted (e.g. OCR confidence too low).
     */
    private final ValidationResult validationResult;
}