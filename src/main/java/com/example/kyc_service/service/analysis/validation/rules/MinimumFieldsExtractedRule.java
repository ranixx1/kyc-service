package com.example.kyc_service.service.analysis.validation.rules;

import com.example.kyc_service.enums.DocumentType;
import com.example.kyc_service.service.analysis.document.ExtractedDocument;
import com.example.kyc_service.service.analysis.validation.ValidationRule;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Validates that the extractor found at least a minimum number of fields.
 *
 * A document where OCR detected the right keywords but the extractor
 * found zero fields likely has too low image quality for reliable analysis.
 *
 * Applies to all document types.
 */
@Component
public class MinimumFieldsExtractedRule implements ValidationRule {

    private static final int MINIMUM_FIELDS = 2;

    @Override
    public boolean supports(DocumentType documentType) {
        return true; // applies to all types
    }

    @Override
    public Optional<String> validate(ExtractedDocument document) {
        int fieldCount = document.toFieldMap().size();

        return fieldCount < MINIMUM_FIELDS
                ? Optional.of(String.format(
                        "Too few fields extracted (%d). Document may be illegible or have low quality.",
                        fieldCount))
                : Optional.empty();
    }
}