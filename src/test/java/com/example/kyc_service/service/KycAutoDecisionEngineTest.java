package com.example.kyc_service.service;

import com.example.kyc_service.enums.DocumentType;
import com.example.kyc_service.model.KycExpectedData;
import com.example.kyc_service.model.KycSubmission;
import com.example.kyc_service.repository.KycExpectedDataRepository;
import com.example.kyc_service.service.analysis.DocumentAnalysis;
import com.example.kyc_service.service.analysis.fraud.FraudDetectionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KycAutoDecisionEngineTest {

    private static final Long USER_ID = 42L;

    @Mock KycExpectedDataRepository expectedDataRepository;

    KycAutoDecisionEngine engine;

    @BeforeEach
    void setUp() {
        engine = new KycAutoDecisionEngine(expectedDataRepository);
    }

    private KycSubmission submission(DocumentType type) {
        return KycSubmission.create(USER_ID, "joao.silva", type, "file-key", "image/jpeg", 1024L);
    }

    private KycExpectedData expectedData(String name, String number) {
        return KycExpectedData.create(USER_ID, DocumentType.IDENTITY_CARD, name, number, 7L, "analyst1");
    }

    private DocumentAnalysis.DocumentAnalysisBuilder baseAnalysis() {
        return DocumentAnalysis.builder()
                .documentType(DocumentType.IDENTITY_CARD)
                .passed(true)
                .fraudResult(FraudDetectionResult.clean());
    }

    @Nested
    @DisplayName("evaluate() — ineligible cases never auto-decide")
    class Ineligible {

        @Test
        @DisplayName("unsupported document type is never auto-decided")
        void unsupportedType() {
            KycSubmission sub = submission(DocumentType.BANK_STATEMENT);
            DocumentAnalysis analysis = baseAnalysis()
                    .documentType(DocumentType.BANK_STATEMENT)
                    .extractedFields(Map.of("holderName", "JOAO SILVA"))
                    .build();

            var decision = engine.evaluate(sub, analysis);

            assertThat(decision.outcome()).isEqualTo(KycAutoDecisionEngine.Outcome.UNSUPPORTED_TYPE);
            assertThat(decision.isApprove()).isFalse();
            assertThat(decision.isReject()).isFalse();
        }

        @Test
        @DisplayName("suspicious fraud result blocks auto-decision, even with matching expected data")
        void fraudSuspiciousBlocks() {
            KycSubmission sub = submission(DocumentType.IDENTITY_CARD);
            DocumentAnalysis analysis = baseAnalysis()
                    .fraudResult(FraudDetectionResult.suspicious(java.util.List.of("Expired document.")))
                    .extractedFields(Map.of("holderName", "JOAO SILVA", "documentNumber", "12345678"))
                    .build();

            var decision = engine.evaluate(sub, analysis);

            assertThat(decision.outcome()).isEqualTo(KycAutoDecisionEngine.Outcome.FRAUD_SUSPECTED);
            assertThat(decision.isApprove()).isFalse();
            assertThat(decision.isReject()).isFalse();
        }

        @Test
        @DisplayName("OCR/validation not passed cannot be auto-decided")
        void notPassed() {
            KycSubmission sub = submission(DocumentType.IDENTITY_CARD);
            DocumentAnalysis analysis = baseAnalysis().passed(false).build();

            var decision = engine.evaluate(sub, analysis);

            assertThat(decision.outcome()).isEqualTo(KycAutoDecisionEngine.Outcome.INSUFFICIENT_DATA);
        }

        @Test
        @DisplayName("no pre-registered expected data means no auto-decision")
        void noExpectedData() {
            KycSubmission sub = submission(DocumentType.IDENTITY_CARD);
            DocumentAnalysis analysis = baseAnalysis()
                    .extractedFields(Map.of("holderName", "JOAO SILVA", "documentNumber", "12345678"))
                    .build();

            when(expectedDataRepository.findFirstByUserIdAndDocumentTypeAndConsumedFalseOrderByCreatedAtAsc(
                    eq(USER_ID), eq(DocumentType.IDENTITY_CARD)))
                    .thenReturn(Optional.empty());

            var decision = engine.evaluate(sub, analysis);

            assertThat(decision.outcome()).isEqualTo(KycAutoDecisionEngine.Outcome.NO_EXPECTED_DATA);
        }

        @Test
        @DisplayName("missing extracted fields cannot be compared")
        void missingExtractedFields() {
            KycSubmission sub = submission(DocumentType.IDENTITY_CARD);
            DocumentAnalysis analysis = baseAnalysis()
                    .extractedFields(Map.of("holderName", "JOAO SILVA")) // no documentNumber
                    .build();

            when(expectedDataRepository.findFirstByUserIdAndDocumentTypeAndConsumedFalseOrderByCreatedAtAsc(
                    eq(USER_ID), eq(DocumentType.IDENTITY_CARD)))
                    .thenReturn(Optional.of(expectedData("JOAO SILVA", "12345678")));

            var decision = engine.evaluate(sub, analysis);

            assertThat(decision.outcome()).isEqualTo(KycAutoDecisionEngine.Outcome.INSUFFICIENT_DATA);
        }
    }

    @Nested
    @DisplayName("evaluate() — comparison against expected data")
    class Comparison {

        @Test
        @DisplayName("exact match on name and document number -> MATCH")
        void exactMatch() {
            KycSubmission sub = submission(DocumentType.IDENTITY_CARD);
            DocumentAnalysis analysis = baseAnalysis()
                    .extractedFields(Map.of("holderName", "Joao Silva", "documentNumber", "12.345.678-9"))
                    .build();

            when(expectedDataRepository.findFirstByUserIdAndDocumentTypeAndConsumedFalseOrderByCreatedAtAsc(
                    eq(USER_ID), eq(DocumentType.IDENTITY_CARD)))
                    .thenReturn(Optional.of(expectedData("JOAO SILVA", "123456789")));

            var decision = engine.evaluate(sub, analysis);

            assertThat(decision.outcome()).isEqualTo(KycAutoDecisionEngine.Outcome.MATCH);
            assertThat(decision.isApprove()).isTrue();
        }

        @Test
        @DisplayName("name normalization ignores accents and case")
        void nameNormalizationHandlesAccents() {
            KycSubmission sub = submission(DocumentType.IDENTITY_CARD);
            DocumentAnalysis analysis = baseAnalysis()
                    .extractedFields(Map.of("holderName", "joão da silva", "documentNumber", "999888777"))
                    .build();

            when(expectedDataRepository.findFirstByUserIdAndDocumentTypeAndConsumedFalseOrderByCreatedAtAsc(
                    eq(USER_ID), eq(DocumentType.IDENTITY_CARD)))
                    .thenReturn(Optional.of(expectedData("JOAO DA SILVA", "999888777")));

            var decision = engine.evaluate(sub, analysis);

            assertThat(decision.outcome()).isEqualTo(KycAutoDecisionEngine.Outcome.MATCH);
        }

        @Test
        @DisplayName("document number mismatch -> MISMATCH (reject)")
        void documentNumberMismatch() {
            KycSubmission sub = submission(DocumentType.IDENTITY_CARD);
            DocumentAnalysis analysis = baseAnalysis()
                    .extractedFields(Map.of("holderName", "JOAO SILVA", "documentNumber", "00000000"))
                    .build();

            when(expectedDataRepository.findFirstByUserIdAndDocumentTypeAndConsumedFalseOrderByCreatedAtAsc(
                    eq(USER_ID), eq(DocumentType.IDENTITY_CARD)))
                    .thenReturn(Optional.of(expectedData("JOAO SILVA", "123456789")));

            var decision = engine.evaluate(sub, analysis);

            assertThat(decision.outcome()).isEqualTo(KycAutoDecisionEngine.Outcome.MISMATCH);
            assertThat(decision.isReject()).isTrue();
            assertThat(decision.reason()).contains("Document number mismatch");
        }

        @Test
        @DisplayName("holder name mismatch -> MISMATCH (reject)")
        void holderNameMismatch() {
            KycSubmission sub = submission(DocumentType.IDENTITY_CARD);
            DocumentAnalysis analysis = baseAnalysis()
                    .extractedFields(Map.of("holderName", "PEDRO ALVES", "documentNumber", "123456789"))
                    .build();

            when(expectedDataRepository.findFirstByUserIdAndDocumentTypeAndConsumedFalseOrderByCreatedAtAsc(
                    eq(USER_ID), eq(DocumentType.IDENTITY_CARD)))
                    .thenReturn(Optional.of(expectedData("JOAO SILVA", "123456789")));

            var decision = engine.evaluate(sub, analysis);

            assertThat(decision.outcome()).isEqualTo(KycAutoDecisionEngine.Outcome.MISMATCH);
            assertThat(decision.reason()).contains("Holder name mismatch");
        }

        @Test
        @DisplayName("uses licenseNumber field for DRIVER_LICENSE")
        void usesLicenseNumberForDriverLicense() {
            KycSubmission sub = submission(DocumentType.DRIVER_LICENSE);
            DocumentAnalysis analysis = baseAnalysis()
                    .documentType(DocumentType.DRIVER_LICENSE)
                    .extractedFields(Map.of("holderName", "JOAO SILVA", "licenseNumber", "CNH123456"))
                    .build();

            when(expectedDataRepository.findFirstByUserIdAndDocumentTypeAndConsumedFalseOrderByCreatedAtAsc(
                    eq(USER_ID), eq(DocumentType.DRIVER_LICENSE)))
                    .thenReturn(Optional.of(expectedData("JOAO SILVA", "CNH123456")));

            var decision = engine.evaluate(sub, analysis);

            assertThat(decision.outcome()).isEqualTo(KycAutoDecisionEngine.Outcome.MATCH);
        }
    }
}