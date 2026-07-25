package com.example.kyc_service.service.ocr;

import com.example.kyc_service.exception.OcrExtractionException;

/**
 * Abstraction for OCR text extraction.
 *
 * A provider has one responsibility: receive raw bytes and a mime type,
 * and return the extracted text. It knows nothing about document types,
 * business rules, or validation logic.
 *
 * This interface makes the OCR engine replaceable Tesseract
 */

public interface OcrProvider {

    /**
     * Extracts raw text from a document.
     *
     * @param fileBytes the document content as bytes
     * @param mimeType  the file mime type (e.g. "image/jpeg", "application/pdf")
     * @return the extracted raw text, never null — returns empty string if nothing found
     * @throws OcrExtractionException if extraction fails due to an unrecoverable error
     */
    String extract(byte[] fileBytes, String mimeType);
}