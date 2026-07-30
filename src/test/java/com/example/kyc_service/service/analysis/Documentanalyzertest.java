package com.example.kyc_service.service.analysis;

import com.example.kyc_service.enums.DocumentType;
import com.example.kyc_service.service.analysis.extractor.DocumentExtractor;
import com.example.kyc_service.service.ocr.OcrProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentAnalyzerTest {

        @Mock
        OcrProvider ocrProvider;

        private final List<DocumentExtractor> extractors = List.of();

        private DocumentAnalyzer analyzer() {
                return new DocumentAnalyzer(ocrProvider, extractors);
        }

        private InputStream stream(String content) {
                return new ByteArrayInputStream(content.getBytes());
        }

        @Nested
        @DisplayName("analyze() — successful OCR")
        class SuccessfulOcr {

                @Test
                @DisplayName("passes ID_CARD when enough patterns match")
                void idCardPasses() {
                        when(ocrProvider.extract(any(), eq("image/jpeg")))
                                        .thenReturn("identity identity card id card identification");

                        DocumentAnalysis result = analyzer().analyze(
                                        stream("irrelevant"),
                                        "image/jpeg",
                                        DocumentType.ID_CARD);

                        assertThat(result.isPassed()).isTrue();
                        assertThat(result.getConfidenceScore()).isGreaterThanOrEqualTo(0.5);
                        assertThat(result.getMatchedPatterns()).isGreaterThanOrEqualTo(4);
                        assertThat(result.getSummary()).contains("Passed");
                }

                @Test
                @DisplayName("fails ID_CARD when too few patterns match")
                void idCardFails() {
                        when(ocrProvider.extract(any(), eq("image/jpeg")))
                                        .thenReturn("lorem ipsum dolor sit amet");

                        DocumentAnalysis result = analyzer().analyze(
                                        stream("irrelevant"),
                                        "image/jpeg",
                                        DocumentType.ID_CARD);

                        assertThat(result.isPassed()).isFalse();
                        assertThat(result.getMatchedPatterns()).isEqualTo(0);
                        assertThat(result.getSummary()).contains("Manual review required");
                }

                @Test
                @DisplayName("passes DRIVER_LICENSE when enough patterns match")
                void driverLicensePasses() {
                        when(ocrProvider.extract(any(), eq("image/jpeg")))
                                        .thenReturn(
                                                        "Driver License Driver's License Driving Licence Driving License");

                        DocumentAnalysis result = analyzer().analyze(
                                        stream("irrelevant"),
                                        "image/jpeg",
                                        DocumentType.DRIVER_LICENSE);

                        assertThat(result.isPassed()).isTrue();
                }

                @Test
                @DisplayName("passes PASSPORT when enough patterns match")
                void passportPasses() {
                        when(ocrProvider.extract(any(), eq("image/jpeg")))
                                        .thenReturn(
                                                        "Passport Passaporte Republic Nationality " +
                                                                        "Date of Birth Place of Birth");

                        DocumentAnalysis result = analyzer().analyze(
                                        stream("irrelevant"),
                                        "image/jpeg",
                                        DocumentType.PASSPORT);

                        assertThat(result.isPassed()).isTrue();
                }

                @Test
                @DisplayName("passes BANK_STATEMENT when enough patterns match")
                void bankStatementPasses() {
                        when(ocrProvider.extract(any(), eq("image/jpeg")))
                                        .thenReturn(
                                                        "Bank Statement Account Statement saldo extrato");

                        DocumentAnalysis result = analyzer().analyze(
                                        stream("irrelevant"),
                                        "image/jpeg",
                                        DocumentType.BANK_STATEMENT);

                        assertThat(result.isPassed()).isTrue();
                }

                @Test
                @DisplayName("passes PAY_SLIP when enough patterns match")
                void paySlipPasses() {
                        when(ocrProvider.extract(any(), eq("image/jpeg")))
                                        .thenReturn("Pay Slip Payslip Salary Slip Paystub");

                        DocumentAnalysis result = analyzer().analyze(
                                        stream("irrelevant"),
                                        "image/jpeg",
                                        DocumentType.PAY_SLIP);

                        assertThat(result.isPassed()).isTrue();
                }

                @Test
                @DisplayName("passes UTILITY_BILL when enough patterns match")
                void utilityBillPasses() {
                        when(ocrProvider.extract(any(), eq("image/jpeg")))
                                        .thenReturn(
                                                        "Utility Bill Electricity Bill Water Bill Gas Bill");

                        DocumentAnalysis result = analyzer().analyze(
                                        stream("irrelevant"),
                                        "image/jpeg",
                                        DocumentType.UTILITY_BILL);

                        assertThat(result.isPassed()).isTrue();
                }

                @Test
                @DisplayName("passes PHONE_BILL when enough patterns match")
                void phoneBillPasses() {
                        when(ocrProvider.extract(any(), eq("image/jpeg")))
                                        .thenReturn(
                                                        "Phone Bill Mobile Bill Telephone Bill Cell Phone Bill");

                        DocumentAnalysis result = analyzer().analyze(
                                        stream("irrelevant"),
                                        "image/jpeg",
                                        DocumentType.PHONE_BILL);

                        assertThat(result.isPassed()).isTrue();
                }

                @Test
                @DisplayName("confidence is calculated correctly")
                void confidenceCalculation() {
                        when(ocrProvider.extract(any(), eq("image/jpeg")))
                                        .thenReturn(
                                                        "identity identity card id card identification");

                        DocumentAnalysis result = analyzer().analyze(
                                        stream("irrelevant"),
                                        "image/jpeg",
                                        DocumentType.ID_CARD);

                        assertThat(result.getConfidenceScore()).isEqualTo(0.5);
                        assertThat(result.getMatchedPatterns()).isEqualTo(4);
                        assertThat(result.getTotalPatterns()).isEqualTo(8);
                        assertThat(result.isPassed()).isTrue();
                }

                @Test
                @DisplayName("returns failed analysis when OCR returns blank text")
                void blankTextFails() {
                        when(ocrProvider.extract(any(), eq("image/jpeg")))
                                        .thenReturn("   ");

                        DocumentAnalysis result = analyzer().analyze(
                                        stream("irrelevant"),
                                        "image/jpeg",
                                        DocumentType.ID_CARD);

                        assertThat(result.isPassed()).isFalse();
                        assertThat(result.getConfidenceScore()).isEqualTo(0.0);
                        assertThat(result.getSummary()).contains("no text");
                }
        }
}