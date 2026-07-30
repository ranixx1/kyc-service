package com.example.kyc_service.service.analysis.extractor;

import com.example.kyc_service.enums.DocumentType;
import com.example.kyc_service.service.analysis.document.ExtractedDocument;

/**
 * Contract for document field extractors.
 *
 * Each implementation is responsible for parsing the raw OCR text
 * of a specific document type and returning a typed model.
 *
 * Extractors use regex patterns and keyword proximity to locate fields.
 * They never throw — if a field cannot be extracted, the value is null.
 * The caller decides what to do with incomplete extractions.
 *
 * To add support for a new document type:
 * 1. Create a new implementation of this interface
 * 2. Annotate it with @Component
 * 3. Spring will auto-discover it — no other changes needed
 */
public interface DocumentExtractor {

    /**
     * Returns true if this extractor handles the given document type.
     */
    boolean supports(DocumentType type);

    /**
     * Extracts structured fields from raw OCR text.
     *
     * @param rawText the full text extracted by the OCR provider
     * @return a typed document model with all extractable fields populated
     */
    ExtractedDocument extract(String rawText);
}