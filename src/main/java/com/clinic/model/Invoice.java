package com.clinic.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Invoice {
    private final Long id;
    private final Long appointmentId;
    private final String invoiceNumber;
    private final LocalDate issueDate;
    private PaymentStatus status;
    private final List<InvoiceItem> items = new ArrayList<>();

    public Invoice(Long id, Long appointmentId, String invoiceNumber, LocalDate issueDate, PaymentStatus status) {
        if (appointmentId == null) {
            throw new IllegalArgumentException("Appointment ID cannot be null.");
        }
        if (invoiceNumber == null || invoiceNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Invoice number cannot be empty.");
        }
        if (issueDate == null) {
            throw new IllegalArgumentException("Issue date cannot be null.");
        }

        this.id = id;
        this.appointmentId = appointmentId;
        this.invoiceNumber = invoiceNumber.trim();
        this.issueDate = issueDate;
        // New invoices start as PENDING unless another status is provided.
        this.status = (status != null) ? status : PaymentStatus.PENDING;
    }

    public Long getId() { return id; }
    public Long getAppointmentId() { return appointmentId; }
    public String getInvoiceNumber() { return invoiceNumber; }
    public LocalDate getIssueDate() { return issueDate;}
    public PaymentStatus getStatus() { return status; }

    public void addItem(InvoiceItem item) {
        if (item == null) {
            throw new IllegalArgumentException("Invoice item cannot be null.");
        }
        item.setInvoiceId(this.id);
        this.items.add(item);
    }

    public List<InvoiceItem> getItems() {
        return Collections.unmodifiableList(this.items);
    }

    public double calculateTotalMur() {
        double total = 0.0;
        for (InvoiceItem item : items) {
            total += item.getAmountMur();
        }
        return total;
    }