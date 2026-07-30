package com.example.kyc_service.service.analysis.document;

import com.example.kyc_service.enums.DocumentType;
import lombok.Builder;
import lombok.Getter;

import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@Builder
public class IdentityDocument implements ExtractedDocument {

    private final String holderName;
    private final String documentNumber;
    private final String dateOfBirth;
    private final String nationality;
    private final String expiryDate;
    private final String issuingAuthority;

    @Override
    public DocumentType getType() {
        return DocumentType.ID_CARD;
    }

    @Override
    public Map<String, String> toFieldMap() {
        Map<String, String> fields = new LinkedHashMap<>();
        putIfPresent(fields, "holderName", holderName);
        putIfPresent(fields, "documentNumber", documentNumber);
        putIfPresent(fields, "dateOfBirth", dateOfBirth);
        putIfPresent(fields, "nationality", nationality);
        putIfPresent(fields, "expiryDate", expiryDate);
        putIfPresent(fields, "issuingAuthority", issuingAuthority);
        return fields;
    }

    private void putIfPresent(Map<String, String> map, String key, String value) {
        if (value != null && !value.isBlank()) map.put(key, value);
    }
}