package com.example.kyc_service.service.analysis.validation;

import com.example.kyc_service.enums.DocumentType;
import com.example.kyc_service.service.analysis.document.ExtractedDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Runs all applicable ValidationRules against an extracted document.
 *
 * The engine collects every rule that supports the given document type,
 * executes them all, and aggregates the results into a single ValidationResult.
 *
 * Rules are auto-discovered by Spring — any @Component implementing
 * ValidationRule is automatically included. The engine itself never needs
 * to change when new rules are added.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ValidationEngine {

    private final List<ValidationRule> rules;

    /**
     * Validates an extracted document against all applicable rules.
     *
     * @param document     the typed extracted document
     * @param documentType the document type (used to filter applicable rules)
     * @return a ValidationResult with all errors found, or passed if none
     */
    public ValidationResult validate(ExtractedDocument document, DocumentType documentType) {
        List<String> errors = rules.stream()
                .filter(rule -> rule.supports(documentType))
                .map(rule -> {
                    try {
                        return rule.validate(document);
                    } catch (Exception e) {
                        log.error("Rule {} threw an unexpected exception for documentType={}: {}",
                                rule.getClass().getSimpleName(), documentType, e.getMessage(), e);
                        return java.util.Optional.<String>empty();
                    }
                })
                .filter(java.util.Optional::isPresent)
                .map(java.util.Optional::get)
                .toList();

        ValidationResult result = errors.isEmpty()
                ? ValidationResult.passed()
                : ValidationResult.failed(errors);

        log.info("Validation complete. documentType={}, valid={}, errors={}",
                documentType, result.valid(), result.errorCount());

        return result;
    }
}