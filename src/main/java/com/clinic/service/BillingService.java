package com.clinic.service;

import com.clinic.dao.AppointmentDao;
import com.clinic.dao.DoctorDao;
import com.clinic.dao.InvoiceDao;
import com.clinic.dao.PatientDao;
import com.clinic.model.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

// Business service coordinating clinic billing, financial aggregation, and payment settlement
// Enforces accounting invariants and calculate multiple visits in a care chain rollups in MUR
public class BillingService {

    public static final double DEFAULT_CONSULTATION_FEE_MUR = 1500.0;
    public static final double STUDENT_HEALTH_SUBSIDY_RATE = 0.0; // 0% VAT for campus clinic

    private final InvoiceDao invoiceDao;
    private final AppointmentDao appointmentDao;
    private final DoctorDao doctorDao;
    private final PatientDao patientDao;
    private final AppointmentService appointmentService;

    public BillingService(InvoiceDao invoiceDao, AppointmentDao appointmentDao,
                          DoctorDao doctorDao, PatientDao patientDao,
                          AppointmentService appointmentService) {
        this.invoiceDao = Objects.requireNonNull(invoiceDao, "InvoiceDao cannot be null");
        this.appointmentDao = Objects.requireNonNull(appointmentDao, "AppointmentDao cannot be null");
        this.doctorDao = Objects.requireNonNull(doctorDao, "DoctorDao cannot be null");
        this.patientDao = Objects.requireNonNull(patientDao, "PatientDao cannot be null");
        this.appointmentService = Objects.requireNonNull(appointmentService, "AppointmentService cannot be null");
    }

    // Generates an itemized invoice for an appointment if one does not already exist
    public Invoice generateInvoiceForAppointment(Long appointmentId) {
        if (appointmentId == null) {
            throw new IllegalArgumentException("Appointment ID cannot be null");
        }

        // Return existing invoice if already generated
        Optional<Invoice> existing = invoiceDao.findByAppointmentId(appointmentId);
        if (existing.isPresent()) {
            return existing.get();
        }

        Appointment appointment = appointmentDao.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found with ID: " + appointmentId));

        // Determine consultation fee based on attending doctor's hourly rate in MUR
        double feeMur = DEFAULT_CONSULTATION_FEE_MUR;
        Optional<Doctor> doctorOpt = doctorDao.findById(appointment.getDoctorId());
        String specialty = "General Consultation";
        if (doctorOpt.isPresent()) {
            Doctor doc = doctorOpt.get();
            specialty = doc.getSpecialty();
            if (doc.getHourlyRate() >= 500.0 && doc.getHourlyRate() <= 10000.0) {
                feeMur = doc.getHourlyRate();
            }
        }

        // Construct unique invoice number: INV-YEAR-0000X
        String invoiceNumber = String.format("INV-%d-%05d", LocalDate.now().getYear(), appointmentId);
        Invoice invoice = new Invoice(null, appointmentId, invoiceNumber, LocalDate.now(), PaymentStatus.PENDING);

        // Add standard consultation fee line item
        invoice.addItem(new InvoiceItem(null, null, "Standard Consultation Fee (" + specialty + ")", feeMur));

        return invoiceDao.save(invoice);
    }

    // Settles payment for an invoice, transitioning status from PENDING to PAID
    public Invoice settlePayment(Long invoiceId, PaymentMethod method, String transactionRef) {
        if (invoiceId == null) {
            throw new IllegalArgumentException("Invoice ID cannot be null");
        }
        if (method == null) {
            throw new IllegalArgumentException("Payment method cannot be null");
        }

        Invoice invoice = invoiceDao.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found with ID: " + invoiceId));

        if (invoice.getStatus() != PaymentStatus.PENDING) {
            throw new IllegalStateException("Only PENDING invoices can be settled. Current status: " + invoice.getStatus());
        }

        // Mark invoice as paid in entity state machine
        invoice.markAsPaid();

        // Update database record
        boolean updated = invoiceDao.updatePaymentStatus(invoiceId, PaymentStatus.PAID);
        if (!updated) {
            throw new RuntimeException("Failed to persist payment settlement in database for invoice: " + invoiceId);
        }

        return invoiceDao.findById(invoiceId).orElse(invoice);
    }

    // Computes recursive financial care chain rollup for an appointment in MUR
    public CareChainSummary getCareChainFinancialRollup(Long appointmentId) {
        if (appointmentId == null) {
            throw new IllegalArgumentException("Appointment ID cannot be null");
        }

        // Locate root of follow-up care chain
        Long currentId = appointmentId;
        while (true) {
            Optional<Appointment> apptOpt = appointmentDao.findById(currentId);
            if (apptOpt.isEmpty() || apptOpt.get().getParentAppointmentId() == null) {
                break;
            }
            currentId = apptOpt.get().getParentAppointmentId();
        }

        return appointmentService.summarizeCareChain(currentId);
    }

    // Calculates aggregate ledger metrics across all recorded invoices
    public LedgerMetrics calculateLedgerMetrics() {
        List<Invoice> allInvoices = invoiceDao.findAll();
        double totalBilled = 0.0;
        double collectedRevenue = 0.0;
        double pendingRevenue = 0.0;
        int paidCount = 0;
        int pendingCount = 0;

        for (Invoice inv : allInvoices) {
            double amount = inv.calculateTotalMur();
            totalBilled += amount;
            if (inv.getStatus() == PaymentStatus.PAID) {
                collectedRevenue += amount;
                paidCount++;
            } else if (inv.getStatus() == PaymentStatus.PENDING) {
                pendingRevenue += amount;
                pendingCount++;
            }
        }

        return new LedgerMetrics(totalBilled, collectedRevenue, pendingRevenue, paidCount, pendingCount);
    }

    // Filter invoices by status (ALL, PENDING, PAID) and search text
    public List<Invoice> filterInvoices(String statusFilter, String searchText) {
        List<Invoice> all = invoiceDao.findAll();
        List<Invoice> results = new ArrayList<>();

        for (Invoice inv : all) {
            // Status match
            boolean matchesStatus = true;
            if ("PENDING".equalsIgnoreCase(statusFilter) && inv.getStatus() != PaymentStatus.PENDING) {
                matchesStatus = false;
            } else if ("PAID".equalsIgnoreCase(statusFilter) && inv.getStatus() != PaymentStatus.PAID) {
                matchesStatus = false;
            }

            if (!matchesStatus) continue;

            // Search query match (invoice number or patient name)
            if (searchText == null || searchText.trim().isEmpty()) {
                results.add(inv);
            } else {
                String term = searchText.trim().toLowerCase();
                boolean matchesNum = inv.getInvoiceNumber().toLowerCase().contains(term);
                boolean matchesPatient = false;

                Optional<Appointment> appt = appointmentDao.findById(inv.getAppointmentId());
                if (appt.isPresent()) {
                    Optional<Patient> p = patientDao.findById(appt.get().getPatientId());
                    if (p.isPresent() && p.get().getFullName().toLowerCase().contains(term)) {
                        matchesPatient = true;
                    }
                }

                if (matchesNum || matchesPatient) {
                    results.add(inv);
                }
            }
        }
        return results;
    }

    // Value object representing summary financial metrics for the billing ledger
    public static class LedgerMetrics {
        private final double totalBilledMur;
        private final double collectedRevenueMur;
        private final double pendingRevenueMur;
        private final int paidCount;
        private final int pendingCount;

        public LedgerMetrics(double totalBilledMur, double collectedRevenueMur,
                             double pendingRevenueMur, int paidCount, int pendingCount) {
            this.totalBilledMur = totalBilledMur;
            this.collectedRevenueMur = collectedRevenueMur;
            this.pendingRevenueMur = pendingRevenueMur;
            this.paidCount = paidCount;
            this.pendingCount = pendingCount;
        }

        public double getTotalBilledMur() { return totalBilledMur; }
        public double getCollectedRevenueMur() { return collectedRevenueMur; }
        public double getPendingRevenueMur() { return pendingRevenueMur; }
        public int getPaidCount() { return paidCount; }
        public int getPendingCount() { return pendingCount; }
    }
}