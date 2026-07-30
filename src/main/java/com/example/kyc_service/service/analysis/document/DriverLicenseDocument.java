package com.example.kyc_service.service.analysis.document;

import com.example.kyc_service.enums.DocumentType;
import lombok.Builder;
import lombok.Getter;

import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@Builder
public class DriverLicenseDocument implements ExtractedDocument {

    private final String holderName;
    private final String licenseNumber;
    private final String dateOfBirth;
    private final String expiryDate;
    private final String licenseCategory;
    private final String issuingAuthority;
    private final String issuingCountry;

    @Override
    public DocumentType getType() {
        return DocumentType.DRIVER_LICENSE;
    }

    @Override
    public Map<String, String> toFieldMap() {
        Map<String, String> fields = new LinkedHashMap<>();
        putIfPresent(fields, "holderName", holderName);
        putIfPresent(fields, "licenseNumber", licenseNumber);
        putIfPresent(fields, "dateOfBirth", dateOfBirth);
        putIfPresent(fields, "expiryDate", expiryDate);
        putIfPresent(fields, "licenseCategory", licenseCategory);
        putIfPresent(fields, "issuingAuthority", issuingAuthority);
        putIfPresent(fields, "issuingCountry", issuingCountry);
        return fields;
    }

    private void putIfPresent(Map<String, String> map, String key, String value) {
        if (value != null && !value.isBlank()) map.put(key, value);
    }
}