package com.example.kyc_service.service.analysis.extractor;

import com.example.kyc_service.enums.DocumentType;
import com.example.kyc_service.service.analysis.document.ExtractedDocument;
import com.example.kyc_service.service.analysis.document.IdentityDocument;
import org.springframework.stereotype.Component;

@Component
public class IdentityDocumentExtractor extends BaseExtractor {

    @Override
    public boolean supports(DocumentType type) {
        return type == DocumentType.ID_CARD;
    }

    @Override
    public ExtractedDocument extract(String rawText) {
        String text = normalize(rawText);

        return IdentityDocument.builder()
                .holderName(extractAfterLabel(text,
                        "name", "full name", "nome", "nome completo", "titular"))
                .documentNumber(extractAfterLabel(text,
                        "document number", "id number", "number", "número", "registro",
                        "doc no", "id no"))
                .dateOfBirth(extractDate(text,
                        "date of birth", "birth date", "born", "dob",
                        "data de nascimento", "nascimento"))
                .nationality(extractAfterLabel(text,
                        "nationality", "nationalité", "nacionalidade"))
                .expiryDate(extractDate(text,
                        "expiry", "expiry date", "expires", "valid until", "valid thru",
                        "validade", "valido até"))
                .issuingAuthority(extractAfterLabel(text,
                        "issuing authority", "issued by", "authority",
                        "órgão emissor", "emissor"))
                .build();
    }
}
