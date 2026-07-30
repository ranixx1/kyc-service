package com.example.kyc_service.service.analysis.document;

import com.example.kyc_service.enums.DocumentType;
import lombok.Builder;
import lombok.Getter;

import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@Builder
public class UtilityBillDocument implements ExtractedDocument {

    private final String holderName;
    private final String serviceAddress;
    private final String serviceProvider;
    private final String billingPeriod;
    private final String totalAmount;
    private final String dueDate;

    @Override
    public DocumentType getType() {
        return DocumentType.UTILITY_BILL;
    }

    @Override
    public Map<String, String> toFieldMap() {
        Map<String, String> fields = new LinkedHashMap<>();
        putIfPresent(fields, "holderName", holderName);
        putIfPresent(fields, "serviceAddress", serviceAddress);
        putIfPresent(fields, "serviceProvider", serviceProvider);
        putIfPresent(fields, "billingPeriod", billingPeriod);
        putIfPresent(fields, "totalAmount", totalAmount);
        putIfPresent(fields, "dueDate", dueDate);
        return fields;
    }

    private void putIfPresent(Map<String, String> map, String key, String value) {
        if (value != null && !value.isBlank()) map.put(key, value);
    }
}