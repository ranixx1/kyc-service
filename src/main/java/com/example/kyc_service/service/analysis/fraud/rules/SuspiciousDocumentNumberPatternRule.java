package com.example.kyc_service.service.analysis.fraud.rules;

import com.example.kyc_service.enums.DocumentType;
import com.example.kyc_service.service.analysis.document.*;
import com.example.kyc_service.service.analysis.fraud.FraudRule;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Fraud indicator: Document number matches a suspicious pattern.
 *
 * Examples of suspicious patterns:
 *   - All zeros: 00000000, 000000000000
 *   - Sequential: 12345678, 1234567890
 *   - Repeating digits: 11111111, 222222222
 *   - Test/placeholder numbers: 999999999, 123456789
 *
 * These patterns are common in:
 *   - Test documents
 *   - Placeholder values
 *   - Fraudulent or dummy submissions
 */
@Component
public class SuspiciousDocumentNumberPatternRule implements FraudRule {

    private static final List<DocumentType> SUPPORTED = List.of(
            DocumentType.IDENTITY_CARD,
            DocumentType.DRIVER_LICENSE,
            DocumentType.PASSPORT
    );

    // Pattern: all same digits (e.g., 00000000, 11111111)
    private static final Pattern ALL_SAME_DIGITS = Pattern.compile("^(\\d)\\1{5,}$");

    // Pattern: sequential digits (e.g., 012345, 123456789)
    private static final Pattern SEQUENTIAL_DIGITS = Pattern.compile("^(0?1234567|12345678|123456789|2345678|3456789).*$");

    // Pattern: common test/placeholder numbers
    private static final Pattern TEST_NUMBERS = Pattern.compile("^(0{6,}|9{6,}|111111|222222|333333|444444|555555|666666|777777|888888)$");

    @Override
    public boolean supports(DocumentType documentType) {
        return SUPPORTED.contains(documentType);
    }

    @Override
    public Optional<String> detect(ExtractedDocument document) throws Exception {
        String number = extractDocumentNumber(document);

        if (number == null || number.isBlank()) {
            return Optional.empty();
        }

        // Remove non-digit characters for pattern matching
        String digits = number.replaceAll("\\D", "");

        if (digits.length() < 6) {
            return Optional.empty();
        }

        if (ALL_SAME_DIGITS.matcher(digits).matches()) {
            return Optional.of("Document number appears to be all identical digits (suspicious pattern).");
        }

        if (SEQUENTIAL_DIGITS.matcher(digits).matches()) {
            return Optional.of("Document number appears to be sequential digits (suspicious pattern).");
        }

        if (TEST_NUMBERS.matcher(digits).matches()) {
            return Optional.of("Document number matches a known test/placeholder pattern.");
        }

        return Optional.empty();
    }

    private String extractDocumentNumber(ExtractedDocument document) {
        return switch (document) {
            case IdentityDocument d -> d.getDocumentNumber();
            case DriverLicenseDocument d -> d.getLicenseNumber();
            case PassportDocument d -> d.getPassportNumber();
            default -> null;
        };
    }
}
