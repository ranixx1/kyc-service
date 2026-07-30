package com.example.kyc_service.service.analysis.extractor;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BaseExtractorTest {

    // Concrete subclass just to test the base methods
    static class TestExtractor extends BaseExtractor {
        @Override
        public boolean supports(com.example.kyc_service.enums.DocumentType type) { return false; }
        @Override
        public com.example.kyc_service.service.analysis.document.ExtractedDocument extract(String rawText) { return null; }

        // Expose protected methods for testing
        String testExtractAfterLabel(String text, String... labels) { return extractAfterLabel(text, labels); }
        String testExtractDate(String text, String... labels) { return extractDate(text, labels); }
        String testExtractAmount(String text, String... labels) { return extractAmount(text, labels); }
        String testExtractByPattern(String text, String regex) { return extractByPattern(text, regex); }
        String testNormalize(String text) { return normalize(text); }
    }

    private final TestExtractor extractor = new TestExtractor();

    @Test
    @DisplayName("extractAfterLabel finds value after colon")
    void extractAfterLabelColon() {
        String text = "Name: John Doe\nDate: 2024-01-01";
        assertThat(extractor.testExtractAfterLabel(text, "Name")).isEqualTo("John Doe");
    }

    @Test
    @DisplayName("extractAfterLabel is case-insensitive")
    void extractAfterLabelCaseInsensitive() {
        String text = "HOLDER NAME: Jane Smith";
        assertThat(extractor.testExtractAfterLabel(text, "holder name")).isEqualTo("Jane Smith");
    }

    @Test
    @DisplayName("extractAfterLabel returns first matching label")
    void extractAfterLabelFirstMatch() {
        String text = "Account Number: 123456";
        String result = extractor.testExtractAfterLabel(text, "account no", "account number");
        assertThat(result).isEqualTo("123456");
    }

    @Test
    @DisplayName("extractAfterLabel returns null when not found")
    void extractAfterLabelNotFound() {
        assertThat(extractor.testExtractAfterLabel("some text", "name")).isNull();
    }

    @Test
    @DisplayName("extractDate finds DD/MM/YYYY format")
    void extractDateSlash() {
        String text = "Date of birth: 15/06/1990";
        assertThat(extractor.testExtractDate(text, "date of birth")).isEqualTo("15/06/1990");
    }

    @Test
    @DisplayName("extractDate finds YYYY-MM-DD format")
    void extractDateDash() {
        String text = "Expiry Date: 2026-12-31";
        assertThat(extractor.testExtractDate(text, "expiry date")).isEqualTo("2026-12-31");
    }

    @Test
    @DisplayName("extractDate finds DD.MM.YYYY format")
    void extractDateDot() {
        String text = "Born: 20.03.1985";
        assertThat(extractor.testExtractDate(text, "born")).isEqualTo("20.03.1985");
    }

    @Test
    @DisplayName("extractAmount finds decimal amount")
    void extractAmount() {
        String text = "Gross Salary: 5,000.00";
        assertThat(extractor.testExtractAmount(text, "gross salary")).isEqualTo("5,000.00");
    }

    @Test
    @DisplayName("extractByPattern extracts IBAN")
    void extractByPattern() {
        String text = "IBAN: GB82 WEST 1234 5698 7654 32";
        String result = extractor.testExtractByPattern(text,
                "\\b([A-Z]{2}\\d{2}[A-Z0-9 ]{4,30})\\b");
        assertThat(result).isNotNull();
        assertThat(result).startsWith("GB");
    }

    @Test
    @DisplayName("extractByPattern returns null when no match")
    void extractByPatternNoMatch() {
        String result = extractor.testExtractByPattern("no iban here", "\\b([A-Z]{2}\\d{2})\\b");
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("normalize collapses multiple spaces")
    void normalize() {
        String text = "Name:   John   Doe\r\nDate: 2024";
        String result = extractor.testNormalize(text);
        assertThat(result).isEqualTo("Name: John Doe\nDate: 2024");
    }
}