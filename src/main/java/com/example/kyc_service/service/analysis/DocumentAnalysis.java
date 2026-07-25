package com.example.kyc_service.service.analysis;

import com.example.kyc_service.enums.DocumentType;
import lombok.Builder;
import lombok.Getter;

/**
 * The result of a full document analysis.
 *
 * This object is the contract between the analysis layer and the rest
 * of the application. It carries everything the system needs to make
 * a decision: the raw OCR text, what was detected, how confident we are,
 * and whether the document passed automatic validation.
 *
 * Future steps will enrich this object with:
 * - extractedFields (Map<String, String>) — Step 3
 * - validationErrors (List<String>)       — Step 5
 * - fraudIndicators (List<String>)        — Step 11
 */
@Getter
@Builder
public class DocumentAnalysis {

    /** The document type that was analyzed. */
    private final DocumentType documentType;

    /** Raw text extracted by the OCR provider. May be null if extraction failed. */
    private final String rawText;

    /**
     * Confidence score between 0.0 and 1.0.
     * Calculated as: matched patterns / total expected patterns.
     */
    private final double confidenceScore;

    /**
     * How many of the expected patterns were found in the extracted text.
     * Useful for debugging and audit.
     */
    private final int matchedPatterns;

    /** Total expected patterns for this document type. */
    private final int totalPatterns;

    /**
     * Whether the document passed automatic validation.
     * true  → status will be IN_PROGRESS (analyst confirms)
     * false → status will be MANUAL (analyst must review)
     */
    private final boolean passed;

    /** Human-readable summary of why the analysis passed or failed. */
    private final String summary;
}