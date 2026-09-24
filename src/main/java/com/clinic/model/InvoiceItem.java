package com.clinic.model;

public class InvoiceItem {
    private final Long id;
    private Long invoiceId;
    private final String description;
    private final double amountMur; // Cost in Mauritian Rupees

    public InvoiceItem(Long id, Long invoiceId, String description, double amountMur) {
        if (description == null || description.trim().isEmpty()) {
            throw new IllegalArgumentException("Item description cannot be empty.");
        }
        // Invoice items cannot have a negative charge.
        if (amountMur < 0.0) {
            throw new IllegalArgumentException("Item amount cannot be negative in MUR.");
        }

        this.id = id;
        this.invoiceId = invoiceId;
        this.description = description.trim();
        this.amountMur = amountMur;
    }

