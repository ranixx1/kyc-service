package com.example.kyc_service.service.analysis.validation.rules;

import com.example.kyc_service.enums.DocumentType;
import com.example.kyc_service.service.analysis.document.*;
import com.example.kyc_service.service.analysis.validation.ValidationRule;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Validates that an expiry or due date was extracted.
 * Identity documents must have an expiry; bills must have a due date.
 */
@Component
public class ExpiryDatePresentRule implements ValidationRule {

    private static final List<DocumentType> SUPPORTED = List.of(
            DocumentType.ID_CARD,
            DocumentType.DRIVER_LICENSE,
            DocumentType.PASSPORT,
            DocumentType.UTILITY_BILL,
            DocumentType.PHONE_BILL
    );

    @Override
    public boolean supports(DocumentType documentType) {
        return SUPPORTED.contains(documentType);
    }

    @Override
    public Optional<String> validate(ExtractedDocument document) {
        String date = switch (document) {
            case IdentityDocument d      -> d.getExpiryDate();
            case DriverLicenseDocument d -> d.getExpiryDate();
            case PassportDocument d      -> d.getExpiryDate();
            case UtilityBillDocument d   -> d.getDueDate();
            case PhoneBillDocument d     -> d.getDueDate();
            default                      -> null;
        };

        return isBlank(date)
                ? Optional.of("Expiry or due date could not be extracted.")
                : Optional.empty();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}