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
                    .thenReturn("identity card identification Federativa");

            DocumentAnalysis result = analyzer.analyze(
                    stream("irrelevant"), "image/jpeg", DocumentType.ID_CARD);

            assertThat(result.isPassed()).isTrue();
            assertThat(result.getConfidenceScore()).isGreaterThanOrEqualTo(0.5);
            assertThat(result.getMatchedPatterns()).isGreaterThanOrEqualTo(2);
            assertThat(result.getSummary()).contains("Passed");
        }

        @Test
        @DisplayName("fails ID_CARD when too few patterns match")
        void idCardFails() {
            when(ocrProvider.extract(any(), eq("image/jpeg")))
                    .thenReturn("lorem ipsum dolor sit amet");

            DocumentAnalysis result = analyzer.analyze(
                    stream("irrelevant"), "image/jpeg", DocumentType.ID_CARD);

            assertThat(result.isPassed()).isFalse();
            assertThat(result.getMatchedPatterns()).isEqualTo(0);
            assertThat(result.getSummary()).contains("Manual review required");
        }

        @Test
        @DisplayName("passes BANK_STATEMENT when patterns match")
        void bankStatementPasses() {
            when(ocrProvider.extract(any(), eq("image/jpeg")))
                    .thenReturn("Bank Statement Account Statement");

            DocumentAnalysis result = analyzer.analyze(
                    stream("irrelevant"),
                    "image/jpeg",
                    DocumentType.BANK_STATEMENT);

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
            // ID_CARD has 8 patterns — matching 2 = 0.25 confidence = passed
            when(ocrProvider.extract(any(), eq("image/jpeg")))
                    .thenReturn("registro geral identidade");

            DocumentAnalysis result = analyzer.analyze(
                    stream("irrelevant"), "image/jpeg", DocumentType.ID_CARD);

            assertThat(result.getConfidenceScore()).isEqualTo(0.25);
            assertThat(result.getMatchedPatterns()).isEqualTo(2);
            assertThat(result.getTotalPatterns()).isEqualTo(8);
            assertThat(result.isPassed()).isTrue();
        }

        @Test
        @DisplayName("returns failed analysis when OCR returns blank text")
        void blankTextFails() {
            when(ocrProvider.extract(any(), eq("image/jpeg"))).thenReturn("   ");

            DocumentAnalysis result = analyzer.analyze(
                    stream("irrelevant"), "image/jpeg", DocumentType.ID_CARD);

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
                    stream("irrelevant"), "image/jpeg", DocumentType.ID_CARD);

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
                    stream("irrelevant"), "image/gif", DocumentType.ID_CARD);

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
                    brokenStream, "image/jpeg", DocumentType.ID_CARD);

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
            String expectedText = "identity card identification";
            when(ocrProvider.extract(any(), any())).thenReturn(expectedText);

            DocumentAnalysis result = analyzer.analyze(
                    stream("irrelevant"), "image/jpeg", DocumentType.ID_CARD);

            assertThat(result.getRawText()).isEqualTo(expectedText);
        }

        @Test
        @DisplayName("documentType is preserved in result")
        void documentTypePreserved() {
            when(ocrProvider.extract(any(), any())).thenReturn("Driver License driver's license");

            DocumentAnalysis result = analyzer.analyze(
                    stream("irrelevant"), "image/jpeg", DocumentType.DRIVER_LICENSE);

            assertThat(result.getDocumentType()).isEqualTo(DocumentType.DRIVER_LICENSE);
        }

        @Test
        void paySlipPasses() {
            when(ocrProvider.extract(any(), eq("image/jpeg")))
                    .thenReturn("Pay Slip Salary Slip");

            DocumentAnalysis result = analyzer.analyze(
                    stream("irrelevant"),
                    "image/jpeg",
                    DocumentType.PAY_SLIP);

            assertThat(result.isPassed()).isTrue();
        }

        @Test
        void utilityBillPasses() {
            when(ocrProvider.extract(any(), eq("image/jpeg")))
                    .thenReturn("Utility Bill Electricity Bill");

            DocumentAnalysis result = analyzer.analyze(
                    stream("irrelevant"),
                    "image/jpeg",
                    DocumentType.UTILITY_BILL);

            assertThat(result.isPassed()).isTrue();
        }

        @Test
        void phoneBillPasses() {
            when(ocrProvider.extract(any(), eq("image/jpeg")))
                    .thenReturn("Phone Bill Mobile Bill");

            DocumentAnalysis result = analyzer.analyze(
                    stream("irrelevant"),
                    "image/jpeg",
                    DocumentType.PHONE_BILL);

            assertThat(result.isPassed()).isTrue();
        }
    }
}