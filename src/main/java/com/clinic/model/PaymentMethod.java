package com.clinic.model;

// Enumeration of supported payment settlement methods for clinic billing
// Supports Mauritian financial settlement channels
public enum PaymentMethod {
    CASH("Cash Settlement"),
    MCB_JUICE("MCB Juice Mobile"),
    DEBIT_CARD("Debit / Credit Card");

    private final String displayName;

    PaymentMethod(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    @Override
    public String toString() {
        return this.displayName;
    }
}