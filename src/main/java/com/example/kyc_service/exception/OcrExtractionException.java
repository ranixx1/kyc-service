package com.example.kyc_service.exception;

/**
 * Thrown when an OCR provider fails to extract text from a document.
 * Signals an unrecoverable error at the extraction level —
 * not a business rule failure.
 */
public class OcrExtractionException extends RuntimeException {

    public OcrExtractionException(String message, Throwable cause) {
        super(message, cause);
    }
}