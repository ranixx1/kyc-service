package com.example.kyc_service.service.analysis.validation;

import java.util.Collections;
import java.util.List;

/**
 * Immutable result of a validation run.
 *
 * Contains all errors found across all applicable rules.
 * An empty error list means all rules passed.
 *
 * Step 7 will extend this with a numeric score (0–100).
 */
public record ValidationResult(
        boolean valid,
        List<String> errors
) {

    public static ValidationResult passed() {
        return new ValidationResult(true, Collections.emptyList());
    }

    public static ValidationResult failed(List<String> errors) {
        return new ValidationResult(false, Collections.unmodifiableList(errors));
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public int errorCount() {
        return errors.size();
    }

    /**
     * Returns a human-readable summary of the validation result.
     */
    public String summary() {
        if (valid) return "Validation passed. No issues found.";
        return String.format("Validation failed. %d issue(s) found: %s",
                errors.size(), String.join("; ", errors));
    }
}