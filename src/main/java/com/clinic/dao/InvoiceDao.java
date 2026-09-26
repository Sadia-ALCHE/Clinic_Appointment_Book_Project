package com.clinic.dao;

import com.clinic.model.Invoice;
import com.clinic.model.PaymentStatus;

import java.util.Optional;

// DAO interface for Invoice entities.
// Provides queries by appointment and invoice number, plus payment status updates.
public interface InvoiceDao extends Dao<Invoice, Long> {
    Optional<Invoice> findByAppointmentId(Long appointmentId);
    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);
    boolean updatePaymentStatus(Long invoiceId, PaymentStatus newStatus);
}