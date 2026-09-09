package com.example.kyc_service.service.analysis.fraud.rules;

import com.example.kyc_service.enums.DocumentType;
import com.example.kyc_service.service.analysis.document.*;
import com.example.kyc_service.service.analysis.fraud.FraudRule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

/**
 * Fraud indicator: Document has expired.
 *
 * This is complementary to ExpiryDateNotExpiredRule (Step 3).
 * That rule rejects expired documents outright as invalid.
 * This rule flags them as a fraud indicator for additional scrutiny.
 *
 * For example, someone might attempt to submit an expired document
 * as part of a fraudulent KYC attempt.
 */
@Component
@Slf4j
public class ExpiredDocumentRule implements FraudRule {

    private static final List<DocumentType> SUPPORTED = List.of(
            DocumentType.IDENTITY_CARD,
            DocumentType.DRIVER_LICENSE,
            DocumentType.PASSPORT
    );

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
        return SUPPORTED.contains(documentType);
    }

    @Override
    public Optional<String> detect(ExtractedDocument document) throws Exception {
        String expiryDate = switch (document) {
            case IdentityDocument d -> d.getExpiryDate();
            case DriverLicenseDocument d -> d.getExpiryDate();
            case PassportDocument d -> d.getExpiryDate();
            default -> null;
        };

        if (expiryDate == null || expiryDate.isBlank()) {
            return Optional.empty();
        }

        LocalDate parsed = tryParse(expiryDate);
        if (parsed == null) {
            return Optional.empty();
        }

        if (parsed.isBefore(LocalDate.now())) {
            return Optional.of(String.format(
                    "Submitted an expired document (expired on %s). Possible fraud indicator.",
                    expiryDate
            ));
        }

        return Optional.empty();
    }

    private LocalDate tryParse(String dateStr) {
        for (DateTimeFormatter formatter : FORMATTERS) {
            try {
                return LocalDate.parse(dateStr.trim(), formatter);
            } catch (DateTimeParseException ignored) {
                // Try next format
            }
        }
        return null;
    }
}
