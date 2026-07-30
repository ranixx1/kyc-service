package com.example.kyc_service.service.analysis.extractor;

import com.example.kyc_service.service.analysis.document.IdentityDocument;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IdentityDocumentExtractorTest {

    private final IdentityDocumentExtractor extractor = new IdentityDocumentExtractor();

    private static final String SAMPLE_ID_CARD =
            "IDENTITY CARD\n" +
            "Name: John Michael Doe\n" +
            "Document Number: AB 123456\n" +
            "Date of Birth: 15/06/1990\n" +
            "Nationality: British\n" +
            "Expiry Date: 31/12/2030\n" +
            "Issuing Authority: HMPO";

    @Test
    @DisplayName("supports ID_CARD only")
    void supportsIdCard() {
        assertThat(extractor.supports(com.example.kyc_service.enums.DocumentType.ID_CARD)).isTrue();
        assertThat(extractor.supports(com.example.kyc_service.enums.DocumentType.PASSPORT)).isFalse();
    }

    @Test
    @DisplayName("extracts holderName")
    void extractsName() {
        IdentityDocument doc = (IdentityDocument) extractor.extract(SAMPLE_ID_CARD);
        assertThat(doc.getHolderName()).isEqualTo("John Michael Doe");
    }

    @Test
    @DisplayName("extracts documentNumber")
    void extractsDocumentNumber() {
        IdentityDocument doc = (IdentityDocument) extractor.extract(SAMPLE_ID_CARD);
        assertThat(doc.getDocumentNumber()).isEqualTo("AB 123456");
    }

    @Test
    @DisplayName("extracts dateOfBirth")
    void extractsDateOfBirth() {
        IdentityDocument doc = (IdentityDocument) extractor.extract(SAMPLE_ID_CARD);
        assertThat(doc.getDateOfBirth()).isEqualTo("15/06/1990");
    }

    @Test
    @DisplayName("extracts nationality")
    void extractsNationality() {
        IdentityDocument doc = (IdentityDocument) extractor.extract(SAMPLE_ID_CARD);
        assertThat(doc.getNationality()).isEqualTo("British");
    }

    @Test
    @DisplayName("extracts expiryDate")
    void extractsExpiryDate() {
        IdentityDocument doc = (IdentityDocument) extractor.extract(SAMPLE_ID_CARD);
        assertThat(doc.getExpiryDate()).isEqualTo("31/12/2030");
    }

    @Test
    @DisplayName("toFieldMap excludes null fields")
    void toFieldMapExcludesNulls() {
        String minimalText = "Name: Jane Doe";
        IdentityDocument doc = (IdentityDocument) extractor.extract(minimalText);
        assertThat(doc.toFieldMap()).containsKey("holderName");
        assertThat(doc.toFieldMap()).doesNotContainKey("documentNumber");
    }

    @Test
    @DisplayName("extracts fields from portuguese document")
    void extractsPortugueseDocument() {
        String ptText =
                "IDENTIDADE\n" +
                "Nome: Maria Silva\n" +
                "Registro: 12.345.678-9\n" +
                "Data de Nascimento: 20/03/1985\n" +
                "Validade: 15/08/2028";

        IdentityDocument doc = (IdentityDocument) extractor.extract(ptText);
        assertThat(doc.getHolderName()).isEqualTo("Maria Silva");
        assertThat(doc.getDateOfBirth()).isEqualTo("20/03/1985");
        assertThat(doc.getExpiryDate()).isEqualTo("15/08/2028");
    }
}