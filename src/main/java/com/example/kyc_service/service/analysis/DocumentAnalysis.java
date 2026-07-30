package com.example.kyc_service.service.analysis;

import com.example.kyc_service.enums.DocumentType;
import com.example.kyc_service.service.analysis.document.ExtractedDocument;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

/**
 * The result of a full document analysis.
 *
 * Step 1 fields: rawText, confidenceScore, matchedPatterns, totalPatterns, passed, summary
 * Step 2 fields: extractedDocument, extractedFields (flattened map for serialization)
 *
 * Future steps will add:
 * - validationErrors (List<String>)  — Step 5
 * - score (int 0–100)                — Step 7
 * - fraudIndicators (List<String>)   — Step 11
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

    /**
     * Typed extracted document — null if extraction was not attempted or failed.
     * Cast to the concrete type when the documentType is known.
     */
    private final ExtractedDocument extractedDocument;

    /**
     * Flattened field map for serialization and display.
     * Populated from extractedDocument.toFieldMap() — null if no extraction occurred.
     */
    private final Map<String, String> extractedFields;
}