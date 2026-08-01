package com.example.kyc_service.service.analysis.validation.rules;

import com.example.kyc_service.enums.DocumentType;
import com.example.kyc_service.service.analysis.document.*;
import com.example.kyc_service.service.analysis.validation.ValidationRule;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Validates that financial documents contain a monetary amount.
 * Applies to bills, statements and pay slips.
 */
@Component
public class AmountPresentRule implements ValidationRule {

    private static final List<DocumentType> SUPPORTED = List.of(
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
        String amount = switch (document) {
            case BankStatementDocument d -> d.getClosingBalance();
            case PaySlipDocument d       -> d.getNetSalary();
            case UtilityBillDocument d   -> d.getTotalAmount();
            case PhoneBillDocument d     -> d.getTotalAmount();
            default                      -> null;
        };

        return isBlank(amount)
                ? Optional.of("No monetary amount could be extracted from the document.")
                : Optional.empty();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}