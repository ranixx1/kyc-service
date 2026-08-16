package com.example.kyc_service.service.analysis.fraud;

import java.util.Collections;
import java.util.List;

/**
 * Immutable result of a fraud detection analysis.
 *
 * Contains all fraud indicators found and any rule errors encountered.
 * - Empty indicators list = no fraud signals detected
 * - Empty ruleErrors list = all rules executed successfully
 *
 * Design:
 *   - suspicious: true if ANY indicator was found
 *   - indicators: list of human-readable fraud signals
 *   - ruleErrors: list of rule execution failures (NOT ignored)
 *
 * Future evolution (Step 5/6):
 *   - Add fraudScore: 0-100 numeric risk assessment
 *   - Add riskLevel: LOW / MEDIUM / HIGH
 *   - Feed results into final KYC decision engine
 */
public record FraudDetectionResult(
        boolean suspicious,
        List<String> indicators,
        List<String> ruleErrors
) {

    public static FraudDetectionResult clean() {
        return new FraudDetectionResult(false, Collections.emptyList(), Collections.emptyList());
    }

    public static FraudDetectionResult suspicious(List<String> indicators) {
        return new FraudDetectionResult(true, Collections.unmodifiableList(indicators), Collections.emptyList());
    }

    public static FraudDetectionResult withRuleErrors(List<String> ruleErrors) {
        return new FraudDetectionResult(true, Collections.emptyList(), Collections.unmodifiableList(ruleErrors));
    }

    public static FraudDetectionResult withBoth(List<String> indicators, List<String> ruleErrors) {
        return new FraudDetectionResult(
                true,
                Collections.unmodifiableList(indicators),
                Collections.unmodifiableList(ruleErrors)
        );
    }

    public boolean hasIndicators() {
        return !indicators.isEmpty();
    }

    public boolean hasRuleErrors() {
        return !ruleErrors.isEmpty();
    }

    public int indicatorCount() {
        return indicators.size();
    }

    public int ruleErrorCount() {
        return ruleErrors.size();
    }

    /**
     * Returns a human-readable summary of the fraud detection result.
     */
    public String summary() {
        if (!suspicious && ruleErrors.isEmpty()) {
            return "Fraud detection passed. No indicators found.";
        }

        StringBuilder sb = new StringBuilder();

        if (hasIndicators()) {
            sb.append(String.format("%d fraud indicator(s) detected: %s",
                    indicators.size(), String.join("; ", indicators)));
        }

        if (hasRuleErrors()) {
            if (hasIndicators()) {
                sb.append(". ");
            }
            sb.append(String.format("%d rule error(s): %s",
                    ruleErrors.size(), String.join("; ", ruleErrors)));
        }

        return sb.toString();
    }
}
