package com.example.kyc_service.service.analysis.fraud.rules;

import com.example.kyc_service.enums.DocumentType;
import com.example.kyc_service.service.analysis.document.ExtractedDocument;
import com.example.kyc_service.service.analysis.fraud.FraudRule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

/**
 * Fraud indicator: Document appears to be very old or from an unusually recent date.
 *
 * Examples:
 *   - Document issued in the 1950s (unrealistic)
 *   - Document issued in the future (impossible)
 *   - Document issued too recently (less than 30 days)
 *
 * This is a deterministic check on extracted dates. For now, it works with whatever
 * date fields are available in the ExtractedDocument.
 *
 * Future enhancements:
 *   - Add issuance date field to ExtractedDocument
 *   - Use image metadata for OCR confidence checks
 */
@Component
@Slf4j
public class SuspiciousExtractionDateRule implements FraudRule {

    private static final int MINIMUM_ISSUE_YEAR = 1950;
    private static final int DAYS_THRESHOLD_FOR_RECENT = 30;

    private static final List<DateTimeFormatter> FORMATTERS = List.of(
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("dd.MM.yyyy"),
            DateTimeFormatter.ofPattern("dd/MM/yy"),
            DateTimeFormatter.ofPattern("MM/dd/yy")
    );

    @Override
    public boolean supports(DocumentType documentType) {
        return true; // Applies to all document types
    }

    @Override
    public Optional<String> detect(ExtractedDocument document) throws Exception {
        // This is a placeholder for deterministic date checks.
        // For now, it returns empty because we don't have issuance dates in ExtractedDocument.
        //
        // Once issuance dates are added to each document type, we can implement:
        //   1. Check if issue date is before 1950
        //   2. Check if issue date is in the future
        //   3. Check if issue date is too recent (< 30 days)

        return Optional.empty();
    }
}
