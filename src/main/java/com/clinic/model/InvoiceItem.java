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

    // Constructor for creating items before invoice ID assignment
    public InvoiceItem(String description, double amountMur) {
        this(null, null, description, amountMur);
    }

    public Long getId() { return id; }
    public Long getInvoiceId() { return invoiceId; }
    public String getDescription() { return description; }
    public double getAmountMur() { return amountMur; }

    public void setInvoiceId(Long invoiceId) {
        this.invoiceId = invoiceId;
    }
}

