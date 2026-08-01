package com.example.kyc_service.service.analysis.validation;

import com.example.kyc_service.enums.DocumentType;
import com.example.kyc_service.service.analysis.document.ExtractedDocument;
import com.example.kyc_service.service.analysis.document.IdentityDocument;
import com.example.kyc_service.service.analysis.validation.rules.DocumentNumberPresentRule;
import com.example.kyc_service.service.analysis.validation.rules.ExpiryDateNotExpiredRule;
import com.example.kyc_service.service.analysis.validation.rules.HolderNamePresentRule;
import com.example.kyc_service.service.analysis.validation.rules.MinimumFieldsExtractedRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ValidationEngineTest {

    private ValidationEngine engine;

    @BeforeEach
    void setUp() {
        engine = new ValidationEngine(List.of(
                new HolderNamePresentRule(),
                new DocumentNumberPresentRule(),
                new ExpiryDateNotExpiredRule(),
                new MinimumFieldsExtractedRule()
        ));
    }

    private IdentityDocument fullDocument() {
        return IdentityDocument.builder()
                .holderName("John Doe")
                .documentNumber("AB123456")
                .dateOfBirth("01/01/1990")
                .nationality("British")
                .expiryDate("31/12/2030")
                .build();
    }

    @Test
    @DisplayName("passes when all rules are satisfied")
    void passesWhenAllRulesSatisfied() {
        ValidationResult result = engine.validate(fullDocument(), DocumentType.ID_CARD);
        assertThat(result.valid()).isTrue();
        assertThat(result.errors()).isEmpty();
    }

    @Test
    @DisplayName("fails when holder name is missing")
    void failsWhenNameMissing() {
        IdentityDocument doc = IdentityDocument.builder()
                .documentNumber("AB123456")
                .expiryDate("31/12/2030")
                .build();

        ValidationResult result = engine.validate(doc, DocumentType.ID_CARD);
        assertThat(result.valid()).isFalse();
        assertThat(result.errors()).anyMatch(e -> e.contains("Holder name"));
    }

    @Test
    @DisplayName("fails when document number is missing")
    void failsWhenDocumentNumberMissing() {
        IdentityDocument doc = IdentityDocument.builder()
                .holderName("John Doe")
                .expiryDate("31/12/2030")
                .build();

        ValidationResult result = engine.validate(doc, DocumentType.ID_CARD);
        assertThat(result.valid()).isFalse();
        assertThat(result.errors()).anyMatch(e -> e.contains("Document number"));
    }

    @Test
    @DisplayName("fails when document is expired")
    void failsWhenExpired() {
        IdentityDocument doc = IdentityDocument.builder()
                .holderName("John Doe")
                .documentNumber("AB123456")
                .expiryDate("01/01/2020")
                .build();

        ValidationResult result = engine.validate(doc, DocumentType.ID_CARD);
        assertThat(result.valid()).isFalse();
        assertThat(result.errors()).anyMatch(e -> e.contains("expired"));
    }

    @Test
    @DisplayName("collects multiple errors in a single run")
    void collectsMultipleErrors() {
        IdentityDocument doc = IdentityDocument.builder().build(); // all nulls

        ValidationResult result = engine.validate(doc, DocumentType.ID_CARD);
        assertThat(result.valid()).isFalse();
        assertThat(result.errors().size()).isGreaterThan(1);
    }

    @Test
    @DisplayName("skips rules that do not support the document type")
    void skipsInapplicableRules() {
        // DocumentNumberPresentRule does not support BANK_STATEMENT
        // So a bank statement without a document number should still pass that rule
        ExtractedDocument doc = com.example.kyc_service.service.analysis.document.BankStatementDocument.builder()
                .holderName("Jane Doe")
                .bankName("HSBC")
                .closingBalance("5000.00")
                .build();

        ValidationResult result = engine.validate(doc, DocumentType.BANK_STATEMENT);
        // Only holderName and amount rules apply — both are satisfied
        assertThat(result.errors()).noneMatch(e -> e.contains("Document number"));
    }

    @Test
    @DisplayName("ValidationResult.summary returns correct text")
    void summaryText() {
        assertThat(ValidationResult.passed().summary()).contains("passed");
        assertThat(ValidationResult.failed(List.of("Error A", "Error B")).summary())
                .contains("2 issue").contains("Error A");
    }
}