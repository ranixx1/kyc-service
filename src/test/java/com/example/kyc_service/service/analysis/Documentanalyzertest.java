package com.example.kyc_service.service.analysis;

import com.example.kyc_service.enums.DocumentType;
import com.example.kyc_service.exception.OcrExtractionException;
import com.example.kyc_service.service.analysis.document.ExtractedDocument;
import com.example.kyc_service.service.analysis.document.IdentityDocument;
import com.example.kyc_service.service.analysis.extractor.DocumentExtractor;
import com.example.kyc_service.service.analysis.validation.ValidationEngine;
import com.example.kyc_service.service.analysis.validation.ValidationResult;
import com.example.kyc_service.service.ocr.OcrProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentAnalyzerTest {

    @Mock OcrProvider ocrProvider;
    @Mock DocumentExtractor extractor;
    @Mock ValidationEngine validationEngine;

    // Construído manualmente para injetar o mock dentro da List<DocumentExtractor>
    DocumentAnalyzer analyzer;

    @BeforeEach
    void setUp() {
        analyzer = new DocumentAnalyzer(ocrProvider, List.of(extractor), validationEngine);
    }

    // Texto com padrões suficientes para passar o threshold de 50% no ID_CARD (8 padrões)
    // "identity", "identity card", "id card", "identification" → 4/8 = 50%
    private static final String ID_CARD_TEXT =
            "identity card id card identification document";

    private InputStream stream(String content) {
        return new ByteArrayInputStream(content.getBytes());
    }

    private ExtractedDocument fullIdentityDoc() {
        return IdentityDocument.builder()
                .holderName("John Doe")
                .documentNumber("AB123456")
                .expiryDate("31/12/2030")
                .nationality("British")
                .build();
    }

    @Nested
    @DisplayName("analyze() — OCR failures")
    class OcrFailures {

        @Test
        @DisplayName("returns failed analysis when OcrProvider throws")
        void ocrProviderThrows() {
            when(ocrProvider.extract(any(), any()))
                    .thenThrow(new OcrExtractionException("Tesseract crashed", new RuntimeException()));

            DocumentAnalysis result = analyzer.analyze(
                    stream("irrelevant"), "image/jpeg", DocumentType.ID_CARD);

            assertThat(result.isPassed()).isFalse();
            assertThat(result.getRawText()).isNull();
            assertThat(result.getSummary()).contains("OCR extraction failed");
            verifyNoInteractions(extractor, validationEngine);
        }

        @Test
        @DisplayName("returns failed analysis for unsupported mime type")
        void unsupportedMimeType() {
            when(ocrProvider.extract(any(), eq("image/gif")))
                    .thenThrow(new IllegalArgumentException("Unsupported mime type: image/gif"));

            DocumentAnalysis result = analyzer.analyze(
                    stream("irrelevant"), "image/gif", DocumentType.ID_CARD);

            assertThat(result.isPassed()).isFalse();
            assertThat(result.getSummary()).contains("Unsupported");
            verifyNoInteractions(extractor, validationEngine);
        }

        @Test
        @DisplayName("returns failed analysis when stream cannot be read")
        void streamReadFails() {
            InputStream brokenStream = new InputStream() {
                @Override
                public int read() throws IOException {
                    throw new IOException("Stream broken");
                }
            };

            DocumentAnalysis result = analyzer.analyze(
                    brokenStream, "image/jpeg", DocumentType.ID_CARD);

            assertThat(result.isPassed()).isFalse();
            assertThat(result.getSummary()).contains("Could not read file content");
            verifyNoInteractions(ocrProvider, extractor, validationEngine);
        }

        @Test
        @DisplayName("returns failed analysis when OCR returns blank text")
        void blankTextFails() {
            when(ocrProvider.extract(any(), any())).thenReturn("   ");

            DocumentAnalysis result = analyzer.analyze(
                    stream("irrelevant"), "image/jpeg", DocumentType.ID_CARD);

            assertThat(result.isPassed()).isFalse();
            assertThat(result.getSummary()).contains("no text");
            verifyNoInteractions(extractor, validationEngine);
        }
    }

    @Nested
    @DisplayName("analyze() — pattern evaluation")
    class PatternEvaluation {

        @Test
        @DisplayName("passes ID_CARD when enough patterns match")
        void idCardPasses() {
            when(ocrProvider.extract(any(), eq("image/jpeg"))).thenReturn(ID_CARD_TEXT);
            when(extractor.supports(DocumentType.ID_CARD)).thenReturn(true);
            when(extractor.extract(any())).thenReturn(fullIdentityDoc());
            when(validationEngine.validate(any(), eq(DocumentType.ID_CARD)))
                    .thenReturn(ValidationResult.passed());

            DocumentAnalysis result = analyzer.analyze(
                    stream("irrelevant"), "image/jpeg", DocumentType.ID_CARD);

            assertThat(result.isPassed()).isTrue();
            assertThat(result.getConfidenceScore()).isGreaterThanOrEqualTo(0.5);
        }

        @Test
        @DisplayName("fails when confidence is below threshold — skips extraction and validation")
        void lowConfidenceSkipsExtraction() {
            when(ocrProvider.extract(any(), any()))
                    .thenReturn("lorem ipsum dolor sit amet");

            DocumentAnalysis result = analyzer.analyze(
                    stream("irrelevant"), "image/jpeg", DocumentType.ID_CARD);

            assertThat(result.isPassed()).isFalse();
            assertThat(result.getExtractedDocument()).isNull();
            assertThat(result.getValidationResult()).isNull();
            verifyNoInteractions(extractor, validationEngine);
        }

        @Test
        @DisplayName("confidence is calculated correctly against total patterns")
        void confidenceCalculation() {
            // ID_CARD has 8 patterns — "identity card id card identification" matches 4 = 0.5
            when(ocrProvider.extract(any(), any())).thenReturn(ID_CARD_TEXT);
            when(extractor.supports(DocumentType.ID_CARD)).thenReturn(true);
            when(extractor.extract(any())).thenReturn(fullIdentityDoc());
            when(validationEngine.validate(any(), any())).thenReturn(ValidationResult.passed());

            DocumentAnalysis result = analyzer.analyze(
                    stream("irrelevant"), "image/jpeg", DocumentType.ID_CARD);

            assertThat(result.getTotalPatterns()).isEqualTo(8);
            assertThat(result.getMatchedPatterns()).isGreaterThanOrEqualTo(4);
            assertThat(result.getConfidenceScore()).isGreaterThanOrEqualTo(0.5);
        }
    }

    @Nested
    @DisplayName("analyze() — extraction and validation pipeline")
    class ExtractionAndValidation {

        @Test
        @DisplayName("populates extractedFields after successful extraction")
        void extractedFieldsPopulated() {
            when(ocrProvider.extract(any(), any())).thenReturn(ID_CARD_TEXT);
            when(extractor.supports(DocumentType.ID_CARD)).thenReturn(true);
            when(extractor.extract(any())).thenReturn(fullIdentityDoc());
            when(validationEngine.validate(any(), any())).thenReturn(ValidationResult.passed());

            DocumentAnalysis result = analyzer.analyze(
                    stream("irrelevant"), "image/jpeg", DocumentType.ID_CARD);

            assertThat(result.getExtractedFields()).isNotNull();
            assertThat(result.getExtractedFields()).containsKey("holderName");
        }

        @Test
        @DisplayName("overrides passed=false when validation fails")
        void validationFailureOverridesPassed() {
            when(ocrProvider.extract(any(), any())).thenReturn(ID_CARD_TEXT);
            when(extractor.supports(DocumentType.ID_CARD)).thenReturn(true);
            when(extractor.extract(any())).thenReturn(fullIdentityDoc());
            when(validationEngine.validate(any(), any()))
                    .thenReturn(ValidationResult.failed(List.of("Document has expired.")));

            DocumentAnalysis result = analyzer.analyze(
                    stream("irrelevant"), "image/jpeg", DocumentType.ID_CARD);

            assertThat(result.isPassed()).isFalse();
            assertThat(result.getValidationResult()).isNotNull();
            assertThat(result.getValidationResult().valid()).isFalse();
            assertThat(result.getValidationResult().errors()).contains("Document has expired.");
        }

        @Test
        @DisplayName("returns base result when no extractor supports the type")
        void noExtractorFound() {
            when(ocrProvider.extract(any(), any())).thenReturn(ID_CARD_TEXT);
            when(extractor.supports(DocumentType.ID_CARD)).thenReturn(false);

            DocumentAnalysis result = analyzer.analyze(
                    stream("irrelevant"), "image/jpeg", DocumentType.ID_CARD);

            assertThat(result.isPassed()).isTrue();
            assertThat(result.getExtractedDocument()).isNull();
            assertThat(result.getValidationResult()).isNull();
            verifyNoInteractions(validationEngine);
        }

        @Test
        @DisplayName("returns base result when extractor throws")
        void extractorThrows() {
            when(ocrProvider.extract(any(), any())).thenReturn(ID_CARD_TEXT);
            when(extractor.supports(DocumentType.ID_CARD)).thenReturn(true);
            when(extractor.extract(any())).thenThrow(new RuntimeException("Extractor crashed"));

            DocumentAnalysis result = analyzer.analyze(
                    stream("irrelevant"), "image/jpeg", DocumentType.ID_CARD);

            assertThat(result.isPassed()).isTrue();
            assertThat(result.getExtractedDocument()).isNull();
            verifyNoInteractions(validationEngine);
        }

        @Test
        @DisplayName("rawText is preserved in final result")
        void rawTextPreserved() {
            when(ocrProvider.extract(any(), any())).thenReturn(ID_CARD_TEXT);
            when(extractor.supports(DocumentType.ID_CARD)).thenReturn(true);
            when(extractor.extract(any())).thenReturn(fullIdentityDoc());
            when(validationEngine.validate(any(), any())).thenReturn(ValidationResult.passed());

            DocumentAnalysis result = analyzer.analyze(
                    stream("irrelevant"), "image/jpeg", DocumentType.ID_CARD);

            assertThat(result.getRawText()).isEqualTo(ID_CARD_TEXT);
        }
    }
}