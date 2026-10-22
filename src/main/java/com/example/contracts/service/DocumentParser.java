package com.example.contracts.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/** Extracts plain text from PDF, DOCX and DOC files. */
@Component
public class DocumentParser {

    public static String detectType(String fileName) {
        String n = fileName == null ? "" : fileName.toLowerCase();
        if (n.endsWith(".pdf")) return "PDF";
        if (n.endsWith(".docx")) return "DOCX";
        if (n.endsWith(".doc")) return "DOC";
        return null;
    }

    public String extractText(byte[] data, String fileType) throws IOException {
        String text = switch (fileType) {
            case "PDF" -> parsePdf(data);
            case "DOCX" -> parseDocx(data);
            case "DOC" -> parseDoc(data);
            default -> throw new IllegalArgumentException("Unsupported file type: " + fileType);
        };
        return text == null ? "" : text.replace("\r", "").trim();
    }

    private String parsePdf(byte[] data) throws IOException {
        try (PDDocument doc = Loader.loadPDF(data)) {
            return new PDFTextStripper().getText(doc);
        }
    }

    private String parseDocx(byte[] data) throws IOException {
        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(data));
             XWPFWordExtractor ex = new XWPFWordExtractor(doc)) {
            return ex.getText();
        }
    }

    private String parseDoc(byte[] data) throws IOException {
        try (HWPFDocument doc = new HWPFDocument(new ByteArrayInputStream(data));
             WordExtractor ex = new WordExtractor(doc)) {
            return ex.getText();
        }
    }
}
