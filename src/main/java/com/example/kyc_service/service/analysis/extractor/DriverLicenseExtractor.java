package com.example.kyc_service.service.analysis.extractor;

import com.example.kyc_service.enums.DocumentType;
import com.example.kyc_service.service.analysis.document.DriverLicenseDocument;
import com.example.kyc_service.service.analysis.document.ExtractedDocument;
import org.springframework.stereotype.Component;

@Component
public class DriverLicenseExtractor extends BaseExtractor {

    @Override
    public boolean supports(DocumentType type) {
        return type == DocumentType.DRIVER_LICENSE;
    }

    @Override
    public ExtractedDocument extract(String rawText) {
        String text = normalize(rawText);

        return DriverLicenseDocument.builder()
                .holderName(extractAfterLabel(text,
                        "name", "full name", "nome", "titular"))
                .licenseNumber(extractAfterLabel(text,
                        "license number", "licence number", "license no",
                        "dl number", "número", "registro cnh", "número cnh"))
                .dateOfBirth(extractDate(text,
                        "date of birth", "dob", "born",
                        "data de nascimento", "nascimento"))
                .expiryDate(extractDate(text,
                        "expiry", "expiry date", "expires", "valid until",
                        "validade", "válido até"))
                .licenseCategory(extractAfterLabel(text,
                        "category", "class", "categories",
                        "categoria", "categorias"))
                .issuingAuthority(extractAfterLabel(text,
                        "issuing authority", "issued by", "detran",
                        "órgão emissor", "emissor"))
                .issuingCountry(extractAfterLabel(text,
                        "country", "issuing country", "país"))
                .build();
    }
}
