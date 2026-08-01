package com.example.kyc_service.service.analysis.validation.rules;

import com.example.kyc_service.enums.DocumentType;
import com.example.kyc_service.service.analysis.document.*;
import com.example.kyc_service.service.analysis.validation.ValidationRule;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Validates that a document number was extracted.
 * Applies only to identity documents where a number is mandatory.
 */
@Component
public class DocumentNumberPresentRule implements ValidationRule {

    private static final List<DocumentType> SUPPORTED = List.of(
            DocumentType.ID_CARD,
            DocumentType.DRIVER_LICENSE,
            DocumentType.PASSPORT
    );

    @Override
    public boolean supports(DocumentType documentType) {
        return SUPPORTED.contains(documentType);
    }

    @Override
    public Optional<String> validate(ExtractedDocument document) {
        String number = switch (document) {
            case IdentityDocument d      -> d.getDocumentNumber();
            case DriverLicenseDocument d -> d.getLicenseNumber();
            case PassportDocument d      -> d.getPassportNumber();
            default                      -> null;
        };

        return isBlank(number)
                ? Optional.of("Document number could not be extracted.")
                : Optional.empty();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}