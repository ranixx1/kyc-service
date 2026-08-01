package com.example.kyc_service.service.analysis.validation.rules;

import com.example.kyc_service.enums.DocumentType;
import com.example.kyc_service.service.analysis.document.*;
import com.example.kyc_service.service.analysis.validation.ValidationRule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

/**
 * Validates that an identity document has not expired.
 *
 * Attempts to parse the expiry date in multiple common formats.
 * If the date cannot be parsed, the rule passes — it's not this rule's
 * job to flag missing dates (that's ExpiryDatePresentRule's responsibility).
 */
@Component
@Slf4j
public class ExpiryDateNotExpiredRule implements ValidationRule {

    private static final List<DocumentType> SUPPORTED = List.of(
            DocumentType.ID_CARD,
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
    public Optional<String> validate(ExtractedDocument document) {
        String expiryDate = switch (document) {
            case IdentityDocument d      -> d.getExpiryDate();
            case DriverLicenseDocument d -> d.getExpiryDate();
            case PassportDocument d      -> d.getExpiryDate();
            default                      -> null;
        };

        if (expiryDate == null || expiryDate.isBlank()) {
            return Optional.empty(); // ExpiryDatePresentRule handles the missing case
        }

        LocalDate parsed = tryParse(expiryDate);
        if (parsed == null) {
            log.warn("Could not parse expiry date '{}' — skipping expiry check.", expiryDate);
            return Optional.empty();
        }

        return parsed.isBefore(LocalDate.now())
                ? Optional.of(String.format("Document has expired on %s.", expiryDate))
                : Optional.empty();
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