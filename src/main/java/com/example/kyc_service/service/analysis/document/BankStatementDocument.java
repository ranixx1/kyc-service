package com.example.kyc_service.service.analysis.document;

import com.example.kyc_service.enums.DocumentType;
import lombok.Builder;
import lombok.Getter;

import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@Builder
public class BankStatementDocument implements ExtractedDocument {

    private final String holderName;
    private final String bankName;
    private final String accountNumber;
    private final String iban;
    private final String statementPeriod;
    private final String openingBalance;
    private final String closingBalance;

    @Override
    public DocumentType getType() {
        return DocumentType.BANK_STATEMENT;
    }

    @Override
    public Map<String, String> toFieldMap() {
        Map<String, String> fields = new LinkedHashMap<>();
        putIfPresent(fields, "holderName", holderName);
        putIfPresent(fields, "bankName", bankName);
        putIfPresent(fields, "accountNumber", accountNumber);
        putIfPresent(fields, "iban", iban);
        putIfPresent(fields, "statementPeriod", statementPeriod);
        putIfPresent(fields, "openingBalance", openingBalance);
        putIfPresent(fields, "closingBalance", closingBalance);
        return fields;
    }

    private void putIfPresent(Map<String, String> map, String key, String value) {
        if (value != null && !value.isBlank()) map.put(key, value);
    }
}