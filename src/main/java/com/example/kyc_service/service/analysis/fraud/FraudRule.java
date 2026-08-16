package com.example.kyc_service.service.analysis.fraud;

import com.example.kyc_service.enums.DocumentType;
import com.example.kyc_service.service.analysis.document.ExtractedDocument;

import java.util.Optional;

/**
 * Contract for a single fraud detection rule.
 *
 * Each implementation checks one specific condition for fraud indicators
 * on an extracted document. Rules are independent and focused.
 *
 * To add a new rule:
 *   1. Create a class implementing this interface
 *   2. Annotate it with @Component
 *   3. Spring auto-discovers it — FraudDetectionEngine picks it up automatically
 *
 * Design principles:
 *   - One rule = one fraud indicator
 *   - Rules should not throw exceptions; if they do, they are treated as rule errors
 *   - Rules declare which document types they apply to via supports()
 *   - Rules return an Optional with a fraud indicator message if suspicious, or empty if clean
 *
 * Note: Unlike ValidationRule, FraudRule errors are NOT silently swallowed.
 * If a rule throws an exception, it generates a RuleError in FraudDetectionResult.
 */
public interface FraudRule {

    /**
     * Returns true if this rule applies to the given document type.
     * Returning false means the rule is skipped for that type.
     */
    boolean supports(DocumentType documentType);

    /**
     * Analyzes the extracted document for fraud indicators.
     *
     * @param document the typed extracted document
     * @return empty Optional if no fraud indicators found, or an Optional containing
     *         a human-readable fraud indicator message if suspicious
     * @throws Exception if the rule encounters an unexpected error (not silently swallowed)
     */
    Optional<String> detect(ExtractedDocument document) throws Exception;
}
