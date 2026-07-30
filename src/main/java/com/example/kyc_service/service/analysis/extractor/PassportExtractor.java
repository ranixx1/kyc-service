package com.example.kyc_service.service.analysis.extractor;

import com.example.kyc_service.enums.DocumentType;
import com.example.kyc_service.service.analysis.document.ExtractedDocument;
import com.example.kyc_service.service.analysis.document.PassportDocument;
import org.springframework.stereotype.Component;

@Component
public class PassportExtractor extends BaseExtractor {

    // MRZ: two lines of 44 characters each (TD3 format used in most passports)
    private static final String MRZ_PATTERN =
            "([A-Z0-9<]{44}\\s*[A-Z0-9<]{44})";

    @Override
    public boolean supports(DocumentType type) {
        return type == DocumentType.PASSPORT;
    }

    @Override
    public ExtractedDocument extract(String rawText) {
        String text = normalize(rawText);

        return PassportDocument.builder()
                .holderName(extractAfterLabel(text,
                        "surname", "given names", "name", "names",
                        "nome", "sobrenome"))
                .passportNumber(extractAfterLabel(text,
                        "passport no", "passport number", "document no",
                        "número do passaporte", "passaporte no"))
                .nationality(extractAfterLabel(text,
                        "nationality", "nationalité", "nacionalidade"))
                .dateOfBirth(extractDate(text,
                        "date of birth", "birth date", "dob",
                        "data de nascimento", "nascimento"))
                .placeOfBirth(extractAfterLabel(text,
                        "place of birth", "birth place",
                        "local de nascimento"))
                .expiryDate(extractDate(text,
                        "date of expiry", "expiry date", "expiration date",
                        "válido até", "validade"))
                .issuingCountry(extractAfterLabel(text,
                        "country of issue", "issuing country", "issued by",
                        "país emissor", "emitido por"))
                .mrz(extractByPattern(text, MRZ_PATTERN))
                .build();
    }
}
