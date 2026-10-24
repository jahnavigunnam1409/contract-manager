package com.example.contracts;

import com.example.contracts.model.ContractStatus;
import org.junit.jupiter.api.Test;

import static com.example.contracts.model.ContractStatus.*;
import static org.junit.jupiter.api.Assertions.*;

class ContractStatusTest {

    @Test
    void allowsOnlyForwardOneStep() {
        assertTrue(DRAFT.canMoveTo(REVIEW));
        assertTrue(REVIEW.canMoveTo(APPROVED));
    }

    @Test
    void rejectsSkippingAndBackwardMoves() {
        assertFalse(DRAFT.canMoveTo(APPROVED));
        assertFalse(REVIEW.canMoveTo(DRAFT));
        assertFalse(APPROVED.canMoveTo(REVIEW));
        assertFalse(APPROVED.canMoveTo(DRAFT));
    }

    @Test
    void nextStatusIsCorrect() {
        assertEquals(REVIEW, DRAFT.next());
        assertEquals(APPROVED, REVIEW.next());
        assertNull(APPROVED.next());
    }
}
