package com.example.kyc_service.service.analysis.fraud;

import com.example.kyc_service.enums.DocumentType;
import com.example.kyc_service.service.analysis.document.ExtractedDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Runs all applicable FraudRules against an extracted document.
 *
 * The engine collects every rule that supports the given document type,
 * executes them all, and aggregates the results into a single FraudDetectionResult.
 *
 * Key differences from ValidationEngine:
 *   1. Rule exceptions are NOT silently swallowed
 *   2. If a rule throws, the error is captured and reported in ruleErrors
 *   3. The document is flagged as suspicious if ANY rule error occurs
 *   4. This prevents dangerous silent failures in fraud detection
 *
 * Rules are auto-discovered by Spring — any @Component implementing
 * FraudRule is automatically included. The engine itself never needs
 * to change when new rules are added.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FraudDetectionEngine {

    private final List<FraudRule> rules;

    /**
     * Analyzes an extracted document against all applicable fraud rules.
     *
     * @param document     the typed extracted document
     * @param documentType the document type (used to filter applicable rules)
     * @return a FraudDetectionResult with all indicators and errors found
     */
    public FraudDetectionResult detect(ExtractedDocument document, DocumentType documentType) {
        List<String> indicators = new ArrayList<>();
        List<String> ruleErrors = new ArrayList<>();

        for (FraudRule rule : rules) {
            if (!rule.supports(documentType)) {
                continue;
            }

            try {
                var result = rule.detect(document);
                if (result.isPresent()) {
                    String indicator = result.get();
                    indicators.add(indicator);
                    log.warn("Fraud indicator detected by {}: {}",
                            rule.getClass().getSimpleName(), indicator);
                }
            } catch (Exception e) {
                String errorMessage = String.format(
                        "Rule %s failed for documentType=%s: %s",
                        rule.getClass().getSimpleName(), documentType, e.getMessage()
                );
                ruleErrors.add(errorMessage);
                log.error("Fraud rule execution error: {}", errorMessage, e);
            }
        }

        FraudDetectionResult result = buildResult(indicators, ruleErrors);
        log.info("Fraud detection complete. documentType={}, suspicious={}, indicators={}, ruleErrors={}",
                documentType, result.suspicious(), result.indicatorCount(), result.ruleErrorCount());

        return result;
    }

    private FraudDetectionResult buildResult(List<String> indicators, List<String> ruleErrors) {
        if (indicators.isEmpty() && ruleErrors.isEmpty()) {
            return FraudDetectionResult.clean();
        }

        if (!ruleErrors.isEmpty() && indicators.isEmpty()) {
            return FraudDetectionResult.withRuleErrors(ruleErrors);
        }

        if (ruleErrors.isEmpty()) {
            return FraudDetectionResult.suspicious(indicators);
        }

        return FraudDetectionResult.withBoth(indicators, ruleErrors);
    }
}
