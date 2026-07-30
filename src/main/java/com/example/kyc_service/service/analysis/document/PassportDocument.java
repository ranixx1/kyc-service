package com.example.kyc_service.service.analysis.document;

import com.example.kyc_service.enums.DocumentType;
import lombok.Builder;
import lombok.Getter;

import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@Builder
public class PassportDocument implements ExtractedDocument {

    private final String holderName;
    private final String passportNumber;
    private final String nationality;
    private final String dateOfBirth;
    private final String placeOfBirth;
    private final String expiryDate;
    private final String issuingCountry;
    private final String mrz; // Machine Readable Zone — bottom two lines of passport

    @Override
    public DocumentType getType() {
        return DocumentType.PASSPORT;
    }

    @Override
    public Map<String, String> toFieldMap() {
        Map<String, String> fields = new LinkedHashMap<>();
        putIfPresent(fields, "holderName", holderName);
        putIfPresent(fields, "passportNumber", passportNumber);
        putIfPresent(fields, "nationality", nationality);
        putIfPresent(fields, "dateOfBirth", dateOfBirth);
        putIfPresent(fields, "placeOfBirth", placeOfBirth);
        putIfPresent(fields, "expiryDate", expiryDate);
        putIfPresent(fields, "issuingCountry", issuingCountry);
        putIfPresent(fields, "mrz", mrz);
        return fields;
    }

    private void putIfPresent(Map<String, String> map, String key, String value) {
        if (value != null && !value.isBlank()) map.put(key, value);
    }
}