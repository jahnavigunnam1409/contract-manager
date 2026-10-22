package com.example.contracts.model;

/** Workflow: DRAFT -> REVIEW -> APPROVED (forward only, one step at a time). */
public enum ContractStatus {
    DRAFT, REVIEW, APPROVED;

    public boolean canMoveTo(ContractStatus next) {
        return next.ordinal() == this.ordinal() + 1;
    }

    public ContractStatus next() {
        return this == APPROVED ? null : values()[ordinal() + 1];
    }
}
