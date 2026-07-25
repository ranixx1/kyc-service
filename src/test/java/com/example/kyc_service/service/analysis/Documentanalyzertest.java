package com.example.kyc_service.service.analysis;

import com.example.kyc_service.enums.DocumentType;
import com.example.kyc_service.exception.OcrExtractionException;
import com.example.kyc_service.service.ocr.OcrProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentAnalyzerTest {

    @Mock
    OcrProvider ocrProvider;

    @InjectMocks
    DocumentAnalyzer analyzer;

    private InputStream stream(String content) {
        return new ByteArrayInputStream(content.getBytes());
    }

    @Nested
    @DisplayName("analyze() — successful OCR")
    class SuccessfulOcr {

        @Test
        @DisplayName("passes RG when enough patterns match")
        void rgPasses() {
            when(ocrProvider.extract(any(), eq("image/jpeg")))
                    .thenReturn("Registro Geral identidade República Federativa");

            DocumentAnalysis result = analyzer.analyze(
                    stream("irrelevant"), "image/jpeg", DocumentType.RG);

            assertThat(result.isPassed()).isTrue();
            assertThat(result.getConfidenceScore()).isGreaterThanOrEqualTo(0.5);
            assertThat(result.getMatchedPatterns()).isGreaterThanOrEqualTo(2);
            assertThat(result.getSummary()).contains("Passed");
        }

        @Test
        @DisplayName("fails RG when too few patterns match")
        void rgFails() {
            when(ocrProvider.extract(any(), eq("image/jpeg")))
                    .thenReturn("lorem ipsum dolor sit amet");

            DocumentAnalysis result = analyzer.analyze(
                    stream("irrelevant"), "image/jpeg", DocumentType.RG);

            assertThat(result.isPassed()).isFalse();
            assertThat(result.getMatchedPatterns()).isEqualTo(0);
            assertThat(result.getSummary()).contains("Manual review required");
        }

        @Test
        @DisplayName("passes CPF when patterns match")
        void cpfPasses() {
            when(ocrProvider.extract(any(), eq("image/jpeg")))
                    .thenReturn("CPF Cadastro de Pessoas Físicas 123.456.789-00");

            DocumentAnalysis result = analyzer.analyze(
                    stream("irrelevant"), "image/jpeg", DocumentType.CPF);

            assertThat(result.isPassed()).isTrue();
        }

        @Test
        @DisplayName("passes PASSPORT when patterns match")
        void passportPasses() {
            when(ocrProvider.extract(any(), eq("image/jpeg")))
                    .thenReturn("PASSPORT passaporte document");

            DocumentAnalysis result = analyzer.analyze(
                    stream("irrelevant"), "image/jpeg", DocumentType.PASSPORT);

            assertThat(result.isPassed()).isTrue();
        }

        @Test
        @DisplayName("confidence is calculated correctly")
        void confidenceCalculation() {
            // RG has 4 patterns — matching 2 = 0.5 confidence = passed
            when(ocrProvider.extract(any(), eq("image/jpeg")))
                    .thenReturn("registro geral identidade");

            DocumentAnalysis result = analyzer.analyze(
                    stream("irrelevant"), "image/jpeg", DocumentType.RG);

            assertThat(result.getConfidenceScore()).isEqualTo(0.5);
            assertThat(result.getMatchedPatterns()).isEqualTo(2);
            assertThat(result.getTotalPatterns()).isEqualTo(4);
            assertThat(result.isPassed()).isTrue();
        }

        @Test
        @DisplayName("returns failed analysis when OCR returns blank text")
        void blankTextFails() {
            when(ocrProvider.extract(any(), eq("image/jpeg"))).thenReturn("   ");

            DocumentAnalysis result = analyzer.analyze(
                    stream("irrelevant"), "image/jpeg", DocumentType.RG);

            assertThat(result.isPassed()).isFalse();
            assertThat(result.getConfidenceScore()).isEqualTo(0.0);
            assertThat(result.getSummary()).contains("no text");
        }
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
                    stream("irrelevant"), "image/jpeg", DocumentType.RG);

            assertThat(result.isPassed()).isFalse();
            assertThat(result.getRawText()).isNull();
            assertThat(result.getSummary()).contains("OCR extraction failed");
        }

        @Test
        @DisplayName("returns failed analysis for unsupported mime type")
        void unsupportedMimeType() {
            when(ocrProvider.extract(any(), eq("image/gif")))
                    .thenThrow(new IllegalArgumentException("Unsupported mime type: image/gif"));

            DocumentAnalysis result = analyzer.analyze(
                    stream("irrelevant"), "image/gif", DocumentType.RG);

            assertThat(result.isPassed()).isFalse();
            assertThat(result.getSummary()).contains("Unsupported");
        }

        @Test
        @DisplayName("returns failed analysis when stream cannot be read")
        void streamReadFails() throws IOException {
            InputStream brokenStream = new InputStream() {
                @Override
                public int read() throws IOException {
                    throw new IOException("Stream broken");
                }
            };

            DocumentAnalysis result = analyzer.analyze(
                    brokenStream, "image/jpeg", DocumentType.RG);

            assertThat(result.isPassed()).isFalse();
            assertThat(result.getSummary()).contains("Could not read file content");
        }
    }

    @Nested
    @DisplayName("DocumentAnalysis fields")
    class AnalysisFields {

        @Test
        @DisplayName("rawText is populated on success")
        void rawTextPopulated() {
            String expectedText = "registro geral identidade república";
            when(ocrProvider.extract(any(), any())).thenReturn(expectedText);

            DocumentAnalysis result = analyzer.analyze(
                    stream("irrelevant"), "image/jpeg", DocumentType.RG);

            assertThat(result.getRawText()).isEqualTo(expectedText);
        }

        @Test
        @DisplayName("documentType is preserved in result")
        void documentTypePreserved() {
            when(ocrProvider.extract(any(), any())).thenReturn("cnh detran habilitação");

            DocumentAnalysis result = analyzer.analyze(
                    stream("irrelevant"), "image/jpeg", DocumentType.CNH);

            assertThat(result.getDocumentType()).isEqualTo(DocumentType.CNH);
        }
    }
}