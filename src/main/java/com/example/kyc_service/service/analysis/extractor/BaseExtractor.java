package com.example.kyc_service.service.analysis.extractor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Shared extraction utilities for all DocumentExtractor implementations.
 *
 * Provides regex-based field extraction helpers that handle common patterns
 * like "Label: Value", date formats, and numeric values.
 * All methods return null when the pattern is not found — never throw.
 */
abstract class BaseExtractor implements DocumentExtractor {

    /**
     * Extracts a value that follows a label on the same line.
     * Example: "Name: John Doe" → "John Doe"
     */
    protected String extractAfterLabel(String text, String... labels) {
        for (String label : labels) {
            Pattern pattern = Pattern.compile(
                    "(?i)" + Pattern.quote(label) + "[:\\s]+([^\\n\\r]{2,60})",
                    Pattern.MULTILINE);
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                String value = matcher.group(1).trim();
                if (!value.isBlank()) return value;
            }
        }
        return null;
    }

    /**
     * Extracts a value matching a specific regex pattern.
     * Returns the first capture group if found.
     */
    protected String extractByPattern(String text, String regex) {
        try {
            Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                return matcher.groupCount() > 0
                        ? matcher.group(1).trim()
                        : matcher.group().trim();
            }
        } catch (Exception ignored) {
            // Invalid regex or match error — return null silently
        }
        return null;
    }

    /**
     * Extracts a date in common formats: DD/MM/YYYY, MM/DD/YYYY, YYYY-MM-DD, DD.MM.YYYY
     */
    protected String extractDate(String text, String... labels) {
        String dateRegex = "(\\d{2}[/.-]\\d{2}[/.-]\\d{2,4}|\\d{4}[/.-]\\d{2}[/.-]\\d{2})";
        for (String label : labels) {
            Pattern pattern = Pattern.compile(
                    "(?i)" + Pattern.quote(label) + "[:\\s]+" + dateRegex,
                    Pattern.MULTILINE);
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) return matcher.group(1).trim();
        }
        return null;
    }

    /**
     * Extracts a monetary amount near a label.
     * Handles formats: 1,234.56 / 1.234,56 / 1234.56
     */
    protected String extractAmount(String text, String... labels) {
        String amountRegex = "([\\d.,]+(?:[.,]\\d{2})?)";
        for (String label : labels) {
            Pattern pattern = Pattern.compile(
                    "(?i)" + Pattern.quote(label) + "[:\\s$€£R$]*" + amountRegex,
                    Pattern.MULTILINE);
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) return matcher.group(1).trim();
        }
        return null;
    }

    /**
     * Normalizes raw OCR text: removes excessive whitespace and normalizes line breaks.
     */
    protected String normalize(String rawText) {
        if (rawText == null) return "";
        return rawText
                .replaceAll("[ \\t]+", " ")
                .replaceAll("\\r\\n|\\r", "\n")
                .trim();
    }
}