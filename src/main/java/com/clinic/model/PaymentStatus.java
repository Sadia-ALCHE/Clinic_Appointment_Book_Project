package com.clinic.model;

public enum PaymentStatus {
    PENDING("Pending Settlement"),
    PARTIALLY_PAID("Partially Paid"),
    PAID("Paid in Full"),
    REFUNDED("Refunded");

    private final String displayName;

    PaymentStatus(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return this.displayName;
    }
}