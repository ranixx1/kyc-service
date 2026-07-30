package com.example.kyc_service.service.analysis.document;

import com.example.kyc_service.enums.DocumentType;
import lombok.Builder;
import lombok.Getter;

import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@Builder
public class PaySlipDocument implements ExtractedDocument {

    private final String employeeName;
    private final String employerName;
    private final String payPeriod;
    private final String grossSalary;
    private final String netSalary;
    private final String taxDeductions;

    @Override
    public DocumentType getType() {
        return DocumentType.PAY_SLIP;
    }

    @Override
    public Map<String, String> toFieldMap() {
        Map<String, String> fields = new LinkedHashMap<>();
        putIfPresent(fields, "employeeName", employeeName);
        putIfPresent(fields, "employerName", employerName);
        putIfPresent(fields, "payPeriod", payPeriod);
        putIfPresent(fields, "grossSalary", grossSalary);
        putIfPresent(fields, "netSalary", netSalary);
        putIfPresent(fields, "taxDeductions", taxDeductions);
        return fields;
    }

    private void putIfPresent(Map<String, String> map, String key, String value) {
        if (value != null && !value.isBlank()) map.put(key, value);
    }
}