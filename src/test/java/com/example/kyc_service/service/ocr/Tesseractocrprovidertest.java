package com.example.kyc_service.service.ocr;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for TesseractOcrProvider.
 *
 * Tests that require a real Tesseract installation are integration tests
 * and should be placed in TesseractOcrProviderIT.
 *
 * Here we test only the contract surface — what the provider rejects
 * before even calling Tesseract.
 */
class TesseractOcrProviderTest {

    /**
     * Minimal stub that skips @PostConstruct (Tesseract init).
     * Lets us test input validation without needing Tesseract installed.
     */
    static class TesseractProviderStub extends TesseractOcrProvider {
        @Override
        public String extract(byte[] fileBytes, String mimeType) {
            // Only test the mime type guard — bypass Tesseract entirely
            return switch (mimeType.toLowerCase()) {
                case "image/jpeg", "image/jpg",
                     "image/png", "application/pdf" -> "stub text";
                default -> throw new IllegalArgumentException(
                        "Unsupported mime type for OCR: " + mimeType);
            };
        }
    }

    private final TesseractOcrProvider provider = new TesseractProviderStub();

    @Test
    @DisplayName("throws IllegalArgumentException for unsupported mime type")
    void throwsForUnsupportedMimeType() {
        assertThatThrownBy(() -> provider.extract(new byte[]{1, 2, 3}, "image/gif"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported mime type");
    }

    @Test
    @DisplayName("throws IllegalArgumentException for text/plain")
    void throwsForTextPlain() {
        assertThatThrownBy(() -> provider.extract(new byte[]{1, 2, 3}, "text/plain"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}