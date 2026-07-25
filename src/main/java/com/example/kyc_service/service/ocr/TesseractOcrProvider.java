package com.example.kyc_service.service.ocr;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.apache.pdfbox.Loader;

import com.example.kyc_service.exception.OcrExtractionException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * OCR provider backed by Tesseract.
 *
 * This class has a single responsibility: convert file bytes into raw text.
 * It has no knowledge of document types, business rules, or validation logic.
 *
 * To swap to a different OCR engine (Google Vision, AWS Textract), create
 * another implementation of OcrProvider and replace this bean.
 */
@Component
@Slf4j
public class TesseractOcrProvider implements OcrProvider {

    private static final float PDF_RENDER_DPI = 200f;
    private static final int PDF_MAX_PAGES = 2;

    @Value("${kyc.ocr.tessdata-path}")
    private String tessdataPath;

    @Value("${kyc.ocr.language}")
    private String ocrLanguage;

    private Tesseract tesseract;

    @PostConstruct
    void init() {
        tesseract = new Tesseract();
        tesseract.setDatapath(tessdataPath);
        tesseract.setLanguage(ocrLanguage);
        tesseract.setPageSegMode(1);
        tesseract.setOcrEngineMode(1);
        log.info("TesseractOcrProvider initialized. datapath={}, language={}", tessdataPath, ocrLanguage);
    }

    @Override
    public String extract(byte[] fileBytes, String mimeType) {
        try {
            return switch (mimeType.toLowerCase()) {
                case "application/pdf" -> extractFromPdf(fileBytes);
                case "image/jpeg", "image/jpg",
                        "image/png" ->
                    extractFromImage(fileBytes);
                default -> throw new IllegalArgumentException(
                        "Unsupported mime type for OCR: " + mimeType);
            };
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new OcrExtractionException(
                    "Tesseract failed to extract text. mimeType=" + mimeType, e);
        }
    }

    // ── Private ───────────────────────────────────────────────────────────────

    private String extractFromImage(byte[] imageBytes) throws IOException, TesseractException {
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
        if (image == null) {
            throw new IOException("Could not decode image bytes.");
        }
        return tesseract.doOCR(image);
    }

    private String extractFromPdf(byte[] pdfBytes) throws IOException, TesseractException {
        var fullText = new StringBuilder();
        try (PDDocument document = Loader.loadPDF(new RandomAccessReadBuffer(pdfBytes))) {
            var renderer = new PDFRenderer(document);
            int pages = Math.min(document.getNumberOfPages(), PDF_MAX_PAGES);
            for (int page = 0; page < pages; page++) {
                BufferedImage image = renderer.renderImageWithDPI(page, PDF_RENDER_DPI, ImageType.GRAY);
                fullText.append(tesseract.doOCR(image)).append("\n");
            }
        }
        return fullText.toString();
    }
}