package com.example.kyc_service.service.analysis;

import com.example.kyc_service.enums.DocumentType;
import com.example.kyc_service.service.analysis.document.ExtractedDocument;
import com.example.kyc_service.service.analysis.extractor.DocumentExtractor;
import com.example.kyc_service.service.analysis.validation.ValidationEngine;
import com.example.kyc_service.service.analysis.validation.ValidationResult;
import com.example.kyc_service.service.ocr.OcrProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Orchestrates the document analysis pipeline.
 *
 * Current pipeline (Step 3):
 *   1. Extract raw text via OcrProvider
 *   2. Evaluate text against expected patterns (confidence score)
 *   3. Run the appropriate DocumentExtractor
 *   4. Run the ValidationEngine against extracted fields
 *   5. Produce a DocumentAnalysis with typed fields and validation result
 *
 * Future (Steps 5–7):
 *   6. Run FraudDetection
 *   7. Compute numeric score (0–100)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentAnalyzer {

    private static final double CONFIDENCE_THRESHOLD = 0.5;

    private final OcrProvider ocrProvider;
    private final List<DocumentExtractor> extractors;
    private final ValidationEngine validationEngine;

    public DocumentAnalysis analyze(InputStream fileStream, String mimeType,
                                    DocumentType documentType) {
        log.info("Starting document analysis. documentType={}, mimeType={}", documentType, mimeType);

        byte[] fileBytes;
        try {
            fileBytes = fileStream.readAllBytes();
        } catch (IOException e) {
            log.error("Failed to read file stream: {}", e.getMessage(), e);
            return failedAnalysis(documentType, null, "Could not read file content.");
        }

        String rawText;
        try {
            rawText = ocrProvider.extract(fileBytes, mimeType);
        } catch (com.example.kyc_service.exception.OcrExtractionException e) {
            log.error("OCR extraction failed. documentType={}: {}", documentType, e.getMessage(), e);
            return failedAnalysis(documentType, null, "OCR extraction failed: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("Unsupported mime type. mimeType={}: {}", mimeType, e.getMessage());
            return failedAnalysis(documentType, null, e.getMessage());
        }

        if (rawText == null || rawText.isBlank()) {
            log.warn("OCR returned no text. documentType={}, mimeType={}", documentType, mimeType);
            return failedAnalysis(documentType, "", "OCR extracted no text from the document.");
        }

        DocumentAnalysis patternResult = evaluate(rawText, documentType);
        if (!patternResult.isPassed()) {
            return patternResult;
        }

        return runExtractionAndValidation(patternResult, rawText, documentType);
    }

    // ── Private ───────────────────────────────────────────────────────────────

    private DocumentAnalysis evaluate(String rawText, DocumentType documentType) {
        String lowerText = rawText.toLowerCase();
        String[] patterns = documentType.expectedPatterns();

        long matchCount = Arrays.stream(patterns)
                .filter(lowerText::contains)
                .count();

        double confidence = (double) matchCount / patterns.length;
        boolean passed = confidence >= CONFIDENCE_THRESHOLD;

        String summary = passed
                ? String.format("Passed. %d/%d patterns matched (confidence: %.0f%%).",
                        matchCount, patterns.length, confidence * 100)
                : String.format("Failed. Only %d/%d patterns matched (confidence: %.0f%%). Manual review required.",
                        matchCount, patterns.length, confidence * 100);

        log.info("Pattern evaluation complete. documentType={}, matched={}/{}, confidence={}, passed={}",
                documentType, matchCount, patterns.length,
                String.format("%.2f", confidence), passed);

        return DocumentAnalysis.builder()
                .documentType(documentType)
                .rawText(rawText)
                .confidenceScore(confidence)
                .matchedPatterns((int) matchCount)
                .totalPatterns(patterns.length)
                .passed(passed)
                .summary(summary)
                .build();
    }

    private DocumentAnalysis runExtractionAndValidation(DocumentAnalysis base,
                                                         String rawText,
                                                         DocumentType documentType) {
        DocumentExtractor extractor = extractors.stream()
                .filter(e -> e.supports(documentType))
                .findFirst()
                .orElse(null);

        if (extractor == null) {
            log.warn("No extractor found for documentType={}.", documentType);
            return base;
        }

        ExtractedDocument extracted;
        try {
            extracted = extractor.extract(rawText);
        } catch (Exception e) {
            log.error("Field extraction failed. documentType={}: {}", documentType, e.getMessage(), e);
            return base;
        }

        Map<String, String> fields = extracted.toFieldMap();
        log.info("Field extraction complete. documentType={}, fieldsExtracted={}", documentType, fields.size());

        ValidationResult validation = validationEngine.validate(extracted, documentType);
        log.info("Validation complete. documentType={}, valid={}, errors={}",
                documentType, validation.valid(), validation.errorCount());

        // If validation fails, override the passed flag — document goes to MANUAL review
        boolean finalPassed = base.isPassed() && validation.valid();
        String finalSummary = finalPassed
                ? base.getSummary()
                : base.getSummary() + " " + validation.summary();

        return DocumentAnalysis.builder()
                .documentType(base.getDocumentType())
                .rawText(base.getRawText())
                .confidenceScore(base.getConfidenceScore())
                .matchedPatterns(base.getMatchedPatterns())
                .totalPatterns(base.getTotalPatterns())
                .passed(finalPassed)
                .summary(finalSummary)
                .extractedDocument(extracted)
                .extractedFields(fields)
                .validationResult(validation)
                .build();
    }

    private DocumentAnalysis failedAnalysis(DocumentType documentType,
                                             String rawText,
                                             String reason) {
        return DocumentAnalysis.builder()
                .documentType(documentType)
                .rawText(rawText)
                .confidenceScore(0.0)
                .matchedPatterns(0)
                .totalPatterns(documentType.expectedPatterns().length)
                .passed(false)
                .summary(reason)
                .build();
    }
}