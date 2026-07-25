package com.example.kyc_service.service.analysis;

import com.example.kyc_service.enums.DocumentType;
import com.example.kyc_service.service.ocr.OcrProvider;
import com.example.kyc_service.exception.OcrExtractionException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

/**
 * Orchestrates the document analysis pipeline.
 *
 * Current pipeline (Step 1):
 *   1. Extract raw text via OcrProvider
 *   2. Evaluate extracted text against expected patterns
 *   3. Produce a DocumentAnalysis result
 *
 * Future pipeline (Steps 3–6):
 *   1. Extract raw text via OcrProvider
 *   2. Classify document type (if not provided)
 *   3. Run the appropriate FieldExtractor
 *   4. Run the ValidationEngine
 *   5. Run FraudDetection
 *   6. Produce a DocumentAnalysis result with score
 *
 * This class knows about the pipeline. It does not know about HTTP,
 * persistence, or Spring Security — those concerns stay in the controller
 * and service layers.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentAnalyzer {

    private static final double CONFIDENCE_THRESHOLD = 0.5;

    private final OcrProvider ocrProvider;

    /**
     * Analyzes a document from an InputStream.
     *
     * Always returns a DocumentAnalysis — never throws. If OCR fails,
     * the result will have passed=false and a descriptive summary,
     * routing the submission to MANUAL review.
     *
     * @param fileStream   the document content stream (caller must close it)
     * @param mimeType     the document mime type
     * @param documentType the declared document type
     * @return the analysis result
     */
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

        return evaluate(rawText, documentType);
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

        log.info("Analysis complete. documentType={}, matched={}/{}, confidence={}, passed={}",
                documentType, matchCount, patterns.length, confidence, passed);

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