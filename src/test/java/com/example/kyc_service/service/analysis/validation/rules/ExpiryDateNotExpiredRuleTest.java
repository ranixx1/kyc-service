package com.example.kyc_service.service.analysis.validation.rules;

import com.example.kyc_service.enums.DocumentType;
import com.example.kyc_service.service.analysis.document.IdentityDocument;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;

class ExpiryDateNotExpiredRuleTest {

    private final ExpiryDateNotExpiredRule rule = new ExpiryDateNotExpiredRule();

    private IdentityDocument docWithExpiry(String date) {
        return IdentityDocument.builder()
                .holderName("Test User")
                .documentNumber("X123")
                .expiryDate(date)
                .build();
    }

    @Test
    @DisplayName("passes for future expiry date DD/MM/YYYY")
    void passesForFutureDate() {
        String future = LocalDate.now().plusYears(2).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        assertThat(rule.validate(docWithExpiry(future))).isEmpty();
    }

    @Test
    @DisplayName("fails for past expiry date")
    void failsForPastDate() {
        assertThat(rule.validate(docWithExpiry("01/01/2020")))
                .isPresent()
                .hasValueSatisfying(msg -> assertThat(msg).contains("expired"));
    }

    @Test
    @DisplayName("passes for YYYY-MM-DD format")
    void parsesIsoFormat() {
        String future = LocalDate.now().plusYears(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        assertThat(rule.validate(docWithExpiry(future))).isEmpty();
    }

    @Test
    @DisplayName("passes silently when expiry date is null")
    void passesWhenNull() {
        IdentityDocument doc = IdentityDocument.builder().holderName("Test").build();
        assertThat(rule.validate(doc)).isEmpty();
    }

    @Test
    @DisplayName("passes silently when date format is unrecognized")
    void passesForUnknownFormat() {
        assertThat(rule.validate(docWithExpiry("DECEMBER 2030"))).isEmpty();
    }

    @Test
    @DisplayName("supports identity document types only")
    void supportsCorrectTypes() {
        assertThat(rule.supports(DocumentType.ID_CARD)).isTrue();
        assertThat(rule.supports(DocumentType.DRIVER_LICENSE)).isTrue();
        assertThat(rule.supports(DocumentType.PASSPORT)).isTrue();
        assertThat(rule.supports(DocumentType.BANK_STATEMENT)).isFalse();
        assertThat(rule.supports(DocumentType.UTILITY_BILL)).isFalse();
    }
}