package com.example.kyc_service.service.analysis.validation.rules;

import com.example.kyc_service.enums.DocumentType;
import com.example.kyc_service.service.analysis.document.*;
import com.example.kyc_service.service.analysis.validation.ValidationRule;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Validates that a holder/employee name was extracted from the document.
 * Applies to all document types that contain a person's name.
 */
@Component
public class HolderNamePresentRule implements ValidationRule {

    private static final List<DocumentType> SUPPORTED = List.of(
            DocumentType.ID_CARD,
            DocumentType.DRIVER_LICENSE,
            DocumentType.PASSPORT,
            DocumentType.BANK_STATEMENT,
            DocumentType.PAY_SLIP,
            DocumentType.UTILITY_BILL,
            DocumentType.PHONE_BILL
    );

    @Override
    public boolean supports(DocumentType documentType) {
        return SUPPORTED.contains(documentType);
    }

    @Override
    public Optional<String> validate(ExtractedDocument document) {
        String name = switch (document) {
            case IdentityDocument d      -> d.getHolderName();
            case DriverLicenseDocument d -> d.getHolderName();
            case PassportDocument d      -> d.getHolderName();
            case BankStatementDocument d -> d.getHolderName();
            case PaySlipDocument d       -> d.getEmployeeName();
            case UtilityBillDocument d   -> d.getHolderName();
            case PhoneBillDocument d     -> d.getHolderName();
            default                      -> null;
        };

        return isBlank(name)
                ? Optional.of("Holder name could not be extracted from the document.")
                : Optional.empty();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}