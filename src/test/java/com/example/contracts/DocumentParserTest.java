package com.example.contracts;

import com.example.contracts.service.DocumentParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class DocumentParserTest {

    private final DocumentParser parser = new DocumentParser();

    @Test
    void detectsFileTypes() {
        assertEquals("PDF", DocumentParser.detectType("a.PDF"));
        assertEquals("DOCX", DocumentParser.detectType("a.docx"));
        assertEquals("DOC", DocumentParser.detectType("a.doc"));
        assertNull(DocumentParser.detectType("a.txt"));
        assertNull(DocumentParser.detectType(null));
    }

    @Test
    void extractsTextFromPdf() throws IOException {
        String text = parser.extractText(TestDocs.pdf(), "PDF");
        assertTrue(text.contains("Payment shall be made within 30 days"), text);
    }

    @Test
    void extractsTextFromDocx() throws IOException {
        String text = parser.extractText(TestDocs.docx(), "DOCX");
        assertTrue(text.contains("terminate this agreement with 60 days"), text);
    }

    @Test
    void corruptPdfThrowsIOException() {
        assertThrows(IOException.class, () -> parser.extractText("not a pdf".getBytes(), "PDF"));
    }
}
