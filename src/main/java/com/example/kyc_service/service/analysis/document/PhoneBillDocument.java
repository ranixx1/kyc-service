package com.example.kyc_service.service.analysis.document;

import com.example.kyc_service.enums.DocumentType;
import lombok.Builder;
import lombok.Getter;

import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@Builder
public class PhoneBillDocument implements ExtractedDocument {

    private final String holderName;
    private final String phoneNumber;
    private final String carrier;
    private final String billingPeriod;
    private final String totalAmount;
    private final String dueDate;
    private final String serviceAddress;

    @Override
    public DocumentType getType() {
        return DocumentType.PHONE_BILL;
    }

    @Override
    public Map<String, String> toFieldMap() {
        Map<String, String> fields = new LinkedHashMap<>();
        putIfPresent(fields, "holderName", holderName);
        putIfPresent(fields, "phoneNumber", phoneNumber);
        putIfPresent(fields, "carrier", carrier);
        putIfPresent(fields, "billingPeriod", billingPeriod);
        putIfPresent(fields, "totalAmount", totalAmount);
        putIfPresent(fields, "dueDate", dueDate);
        putIfPresent(fields, "serviceAddress", serviceAddress);
        return fields;
    }

    private void putIfPresent(Map<String, String> map, String key, String value) {
        if (value != null && !value.isBlank()) map.put(key, value);
    }
}