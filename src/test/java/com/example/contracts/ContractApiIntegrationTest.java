package com.example.contracts;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** End-to-end tests of the REST API (real Spring context, in-memory H2, temp upload folder). */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
        "app.storage.dir=target/test-uploads"
})
@AutoConfigureMockMvc
class ContractApiIntegrationTest {

    @Autowired MockMvc mvc;

    private long upload(String name, String type, byte[] bytes, String title) throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", name, type, bytes);
        String body = mvc.perform(multipart("/api/contracts").file(file).param("title", title))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private void setStatus(long id, String s, int expected) throws Exception {
        mvc.perform(patch("/api/contracts/{id}/status", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"" + s + "\"}"))
                .andExpect(status().is(expected));
    }

    @Test
    void createPdfAndFetchWithParsedText() throws Exception {
        long id = upload("nda.pdf", "application/pdf", TestDocs.pdf(), "NDA");
        mvc.perform(get("/api/contracts/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("NDA"))
                .andExpect(jsonPath("$.fileType").value("PDF"))
                .andExpect(jsonPath("$.extractedText").value(org.hamcrest.Matchers.containsString("30 days")));
    }

    @Test
    void createDocx() throws Exception {
        long id = upload("deal.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                TestDocs.docx(), "Deal");
        mvc.perform(get("/api/contracts/{id}", id))
                .andExpect(jsonPath("$.fileType").value("DOCX"))
                .andExpect(jsonPath("$.wordCount").isNumber());
    }

    @Test
    void listContainsUploadedContract() throws Exception {
        upload("a.pdf", "application/pdf", TestDocs.pdf(), "Listed contract");
        mvc.perform(get("/api/contracts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.title=='Listed contract')]").exists());
    }

    @Test
    void downloadReturnsOriginalBytes() throws Exception {
        byte[] original = TestDocs.pdf();
        long id = upload("orig.pdf", "application/pdf", original, "Orig");
        byte[] downloaded = mvc.perform(get("/api/contracts/{id}/download", id))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        assertArrayEquals(original, downloaded);
    }

    @Test
    void statusWorkflowDraftReviewApproved() throws Exception {
        long id = upload("flow.pdf", "application/pdf", TestDocs.pdf(), "Flow");
        setStatus(id, "REVIEW", 200);
        setStatus(id, "APPROVED", 200);
        mvc.perform(get("/api/contracts/{id}", id)).andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    void invalidTransitionIsRejectedWith409() throws Exception {
        long id = upload("bad.pdf", "application/pdf", TestDocs.pdf(), "Bad");
        setStatus(id, "APPROVED", 409);                 // DRAFT -> APPROVED not allowed
        mvc.perform(get("/api/contracts/{id}", id)).andExpect(jsonPath("$.status").value("DRAFT"));
        setStatus(id, "REVIEW", 200);
        setStatus(id, "DRAFT", 409);                    // backwards not allowed
    }

    @Test
    void unknownStatusValueIs400() throws Exception {
        long id = upload("x.pdf", "application/pdf", TestDocs.pdf(), "X");
        setStatus(id, "FINISHED", 400);
    }

    @Test
    void askQuestionReturnsRelevantAnswer() throws Exception {
        long id = upload("qa.pdf", "application/pdf", TestDocs.pdf(), "QA");
        mvc.perform(post("/api/contracts/{id}/ask", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"When is payment due?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value(org.hamcrest.Matchers.containsString("30 days")));
    }

    @Test
    void blankQuestionIs400() throws Exception {
        long id = upload("q.pdf", "application/pdf", TestDocs.pdf(), "Q");
        mvc.perform(post("/api/contracts/{id}/ask", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unsupportedFileTypeIs400() throws Exception {
        MockMultipartFile txt = new MockMultipartFile("file", "notes.txt", "text/plain", "hello".getBytes());
        mvc.perform(multipart("/api/contracts").file(txt)).andExpect(status().isBadRequest());
    }

    @Test
    void emptyFileIs400() throws Exception {
        MockMultipartFile empty = new MockMultipartFile("file", "e.pdf", "application/pdf", new byte[0]);
        mvc.perform(multipart("/api/contracts").file(empty)).andExpect(status().isBadRequest());
    }

    @Test
    void corruptPdfIs422() throws Exception {
        MockMultipartFile bad = new MockMultipartFile("file", "bad.pdf", "application/pdf", "garbage".getBytes());
        mvc.perform(multipart("/api/contracts").file(bad)).andExpect(status().isUnprocessableEntity());
    }

    @Test
    void unknownContractIs404() throws Exception {
        mvc.perform(get("/api/contracts/{id}", 999999)).andExpect(status().isNotFound());
        setStatus(999999, "REVIEW", 404);
    }
}
