package com.example.contracts;

import com.example.contracts.service.QaService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class QaServiceTest {

    private final QaService qa = new QaService();
    private final String text = String.join(" ", TestDocs.LINES);

    @Test
    void findsPaymentTerm() {
        var a = qa.ask(text, "When is payment due?");
        assertTrue(a.answer().contains("30 days"), a.answer());
    }

    @Test
    void findsTerminationNotice() {
        var a = qa.ask(text, "How many days notice is needed to terminate?");
        assertTrue(a.answer().contains("60 days"), a.answer());
    }

    @Test
    void handlesPluralsViaStemming() {
        var a = qa.ask(text, "Which payments are mentioned?");
        assertTrue(a.answer().contains("Payment"), a.answer());
    }

    @Test
    void returnsUpToThreeMatchesBestFirst() {
        var a = qa.ask(text, "days notice terminate payment");
        assertFalse(a.matches().isEmpty());
        assertTrue(a.matches().size() <= 3);
        for (int i = 1; i < a.matches().size(); i++) {
            assertTrue(a.matches().get(i - 1).score() >= a.matches().get(i).score());
        }
    }

    @Test
    void saysNothingFoundForUnrelatedQuestion() {
        var a = qa.ask(text, "Who is the CEO of Mars?");
        assertTrue(a.matches().isEmpty());
        assertTrue(a.answer().toLowerCase().contains("couldn't find"));
    }

    @Test
    void handlesEmptyTextAndEmptyQuestion() {
        assertTrue(qa.ask("", "payment").matches().isEmpty());
        assertTrue(qa.ask(text, "the and of").matches().isEmpty());
    }
}
