package com.example.kyc_service.service.analysis;

import com.example.kyc_service.enums.DocumentType;
import com.example.kyc_service.exception.OcrExtractionException;
import com.example.kyc_service.service.analysis.document.ExtractedDocument;
import com.example.kyc_service.service.analysis.extractor.DocumentExtractor;
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
 * Current pipeline (Step 2):
 *   1. Extract raw text via OcrProvider
 *   2. Evaluate text against expected patterns (confidence score)
 *   3. Run the appropriate DocumentExtractor
 *   4. Produce a DocumentAnalysis with typed fields
 *
 * Future pipeline (Steps 5–7):
 *   5. Run ValidationEngine against extracted fields
 *   6. Run FraudDetection
 *   7. Produce a score (0–100) alongside the pass/fail decision
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentAnalyzer {

    private static final double CONFIDENCE_THRESHOLD = 0.5;

    private final OcrProvider ocrProvider;
    private final List<DocumentExtractor> extractors;

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
        } catch (OcrExtractionException e) {
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

        // Only run field extraction if OCR confidence passed the threshold
        if (!patternResult.isPassed()) {
            log.info("Confidence too low for field extraction. documentType={}, confidence={}",
                    documentType, patternResult.getConfidenceScore());
            return patternResult;
        }

        return runExtraction(patternResult, rawText, documentType);
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

    private DocumentAnalysis runExtraction(DocumentAnalysis base, String rawText,
                                           DocumentType documentType) {
        DocumentExtractor extractor = extractors.stream()
                .filter(e -> e.supports(documentType))
                .findFirst()
                .orElse(null);

        if (extractor == null) {
            log.warn("No extractor found for documentType={}. Returning pattern result.", documentType);
            return base;
        }

        try {
            ExtractedDocument extracted = extractor.extract(rawText);
            Map<String, String> fields = extracted.toFieldMap();

            log.info("Field extraction complete. documentType={}, fieldsExtracted={}",
                    documentType, fields.size());

            return DocumentAnalysis.builder()
                    .documentType(base.getDocumentType())
                    .rawText(base.getRawText())
                    .confidenceScore(base.getConfidenceScore())
                    .matchedPatterns(base.getMatchedPatterns())
                    .totalPatterns(base.getTotalPatterns())
                    .passed(base.isPassed())
                    .summary(base.getSummary())
                    .extractedDocument(extracted)
                    .extractedFields(fields)
                    .build();

        } catch (Exception e) {
            log.error("Field extraction failed. documentType={}: {}", documentType, e.getMessage(), e);
            // Extraction failure does not fail the analysis — return the base result
            return base;
        }
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