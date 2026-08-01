package com.example.kyc_service.service.analysis.validation;

import com.example.kyc_service.enums.DocumentType;
import com.example.kyc_service.service.analysis.document.ExtractedDocument;

import java.util.Optional;

/**
 * Contract for a single validation rule.
 *
 * Each implementation checks one specific condition on an extracted document.
 * Rules are independent — they do not know about each other.
 *
 * To add a new rule:
 *   1. Create a class implementing this interface
 *   2. Annotate it with @Component
 *   3. Spring auto-discovers it — ValidationEngine picks it up automatically
 *
 * Design principles:
 *   - One rule = one responsibility
 *   - Rules never throw — they return an Optional with the error message
 *   - Rules declare which document types they apply to via supports()
 */
public interface ValidationRule {

    /**
     * Returns true if this rule applies to the given document type.
     * Returning false means the rule is skipped for that type.
     */
    boolean supports(DocumentType documentType);

    /**
     * Validates the extracted document.
     *
     * @param document the typed extracted document
     * @return empty Optional if the rule passes, or an Optional containing
     *         a human-readable error message if it fails
     */
    Optional<String> validate(ExtractedDocument document);
}