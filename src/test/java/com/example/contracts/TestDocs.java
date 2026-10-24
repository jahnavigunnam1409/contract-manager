package com.example.contracts;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/** Builds small PDF / DOCX files in memory for tests. */
public final class TestDocs {
    private TestDocs() {}

    public static final String[] LINES = {
        "This agreement is made between Acme Corp and Beta Ltd for software services.",
        "Payment shall be made within 30 days of receipt of invoice.",
        "Either party may terminate this agreement with 60 days written notice.",
        "All disclosed information shall remain confidential for five years."
    };

    public static byte[] pdf() throws IOException {
        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
                cs.newLineAtOffset(30, 700);
                for (String line : LINES) {
                    cs.showText(line);
                    cs.newLineAtOffset(0, -20);
                }
                cs.endText();
            }
            doc.save(out);
            return out.toByteArray();
        }
    }

    public static byte[] docx() throws IOException {
        try (XWPFDocument doc = new XWPFDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            for (String line : LINES) {
                doc.createParagraph().createRun().setText(line);
            }
            doc.write(out);
            return out.toByteArray();
        }
    }
}
