package com.clinic.service;

import com.clinic.dao.AppointmentDao;
import com.clinic.dao.DoctorDao;
import com.clinic.dao.InvoiceDao;
import com.clinic.dao.PatientDao;
import com.clinic.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

// Automated audit test suite verifying financial arithmetic and payment state machine invariants
public class FinancialLedgerAuditTest {

    private InMemoryInvoiceDao invoiceDao;
    private InMemoryAppointmentDao appointmentDao;
    private InMemoryDoctorDao doctorDao;
    private InMemoryPatientDao patientDao;
    private BillingService billingService;

    @BeforeEach
    public void setUp() {
        invoiceDao = new InMemoryInvoiceDao();
        appointmentDao = new InMemoryAppointmentDao();
        doctorDao = new InMemoryDoctorDao();
        patientDao = new InMemoryPatientDao();
        AppointmentService appointmentService = new AppointmentService(appointmentDao, doctorDao);
        billingService = new BillingService(invoiceDao, appointmentDao, doctorDao, patientDao, appointmentService);

        doctorDao.save(new Doctor(1L, "Dr. Sarah Mensah", "General Medicine", "+230 5842 1001", "sm@medicare.mu", 1500.0));
        patientDao.save(new Patient(1L, "Adebayo Ogunlesi", "adebayo@alche.edu.mu", "+230 5842 1099", LocalDate.of(2003, 5, 14)));
        appointmentDao.save(new Appointment(100L, 1L, 1L, LocalDateTime.now(),
                AppointmentStatus.COMPLETED, "Clinical Triage", null, AppointmentType.STANDARD_CONSULTATION));
    }

    @Test
    @DisplayName("Should calculate exact invoice total with itemized diagnostic charges in MUR")
    public void shouldCalculateExactInvoiceTotalWithItemizedChargesInMur() {
        Invoice invoice = billingService.generateInvoiceForAppointment(100L);
        invoice.addItem(new InvoiceItem(null, invoice.getId(), "Blood Glucose Panel", 1200.0));
        invoice.addItem(new InvoiceItem(null, invoice.getId(), "Resting ECG", 1150.0));
        invoice.addItem(new InvoiceItem(null, invoice.getId(), "Sterile Dressing", 850.0));

        // 1500.0 (consult) + 1200.0 + 1150.0 + 850.0 = 4,700.0 MUR (matches Paper Board 07)
        assertEquals(4700.0, invoice.calculateTotalMur());
    }

    @Test
    @DisplayName("FSM: Should transition status from PENDING to PAID upon payment settlement")
    public void shouldEnforceFiniteStateMachinePaymentTransitions() {
        Invoice invoice = billingService.generateInvoiceForAppointment(100L);
        assertEquals(PaymentStatus.PENDING, invoice.getStatus());

        Invoice settled = billingService.settlePayment(invoice.getId(), PaymentMethod.MCB_JUICE, "JUICE-58421099");

        assertEquals(PaymentStatus.PAID, settled.getStatus());
        assertTrue(settled.getStatus().isSettled());
    }

    @Test
    @DisplayName("FSM: Should reject payment settlement on an already settled invoice")
    public void shouldRejectDuplicatePaymentSettlementOnPaidInvoice() {
        Invoice invoice = billingService.generateInvoiceForAppointment(100L);
        billingService.settlePayment(invoice.getId(), PaymentMethod.CASH, "CASH-001");

        assertThrows(IllegalStateException.class, () ->
                billingService.settlePayment(invoice.getId(), PaymentMethod.DEBIT_CARD, "CARD-002"));
    }

    @Test
    @DisplayName("Metrics: Should compute accurate accounts receivable ledger totals")
    public void shouldComputeAccurateAccountsReceivableMetrics() {
        // Invoice 1: 1500.0 MUR settled
        Invoice inv1 = billingService.generateInvoiceForAppointment(100L);
        billingService.settlePayment(inv1.getId(), PaymentMethod.CASH, "CASH-01");

        // Invoice 2: 2500.0 MUR pending
        doctorDao.save(new Doctor(2L, "Dr. Jean-Luc Pierre", "Cardiology", "+230 5842 1002", "jl@medicare.mu", 2500.0));
        appointmentDao.save(new Appointment(101L, 1L, 2L, LocalDateTime.now(),
                AppointmentStatus.CONFIRMED, "Cardiology Consult", null, AppointmentType.STANDARD_CONSULTATION));
        billingService.generateInvoiceForAppointment(101L);

        BillingService.LedgerMetrics metrics = billingService.calculateLedgerMetrics();

        assertEquals(4000.0, metrics.getTotalBilledMur());
        assertEquals(1500.0, metrics.getCollectedRevenueMur());
        assertEquals(2500.0, metrics.getPendingRevenueMur());
        assertEquals(1, metrics.getPaidCount());
        assertEquals(1, metrics.getPendingCount());
    }

    // In-Memory Test Doubles
    private static class InMemoryInvoiceDao implements InvoiceDao {
        private final Map<Long, Invoice> map = new HashMap<>();
        private long seq = 1L;
        @Override public Invoice save(Invoice e) {
            Long id = e.getId() != null ? e.getId() : seq++;
            Invoice inv = new Invoice(id, e.getAppointmentId(), e.getInvoiceNumber(), e.getIssueDate(), e.getStatus());
            for (InvoiceItem item : e.getItems()) inv.addItem(item);
            map.put(id, inv);
            return inv;
        }
        @Override public Optional<Invoice> findById(Long id) { return Optional.ofNullable(map.get(id)); }
        @Override public List<Invoice> findAll() { return new ArrayList<>(map.values()); }
        @Override public boolean deleteById(Long id) { return map.remove(id) != null; }
        @Override public Optional<Invoice> findByAppointmentId(Long id) {
            return map.values().stream().filter(i -> i.getAppointmentId().equals(id)).findFirst();
        }
        @Override public Optional<Invoice> findByInvoiceNumber(String num) {
            return map.values().stream().filter(i -> i.getInvoiceNumber().equalsIgnoreCase(num)).findFirst();
        }
        @Override public boolean updatePaymentStatus(Long id, PaymentStatus status) {
            Invoice inv = map.get(id);
            if (inv == null) return false;
            if (status == PaymentStatus.PAID) inv.markAsPaid();
            return true;
        }
    }

    private static class InMemoryAppointmentDao implements AppointmentDao {
        private final Map<Long, Appointment> map = new HashMap<>();
        @Override public Appointment save(Appointment e) { map.put(e.getId(), e); return e; }
        @Override public Optional<Appointment> findById(Long id) { return Optional.ofNullable(map.get(id)); }
        @Override public List<Appointment> findAll() { return new ArrayList<>(map.values()); }
        @Override public boolean deleteById(Long id) { return map.remove(id) != null; }
        @Override public List<Appointment> findByDoctorIdAndDate(Long dId, LocalDate date) { return Collections.emptyList(); }
        @Override public List<Appointment> findByPatientId(Long pId) { return Collections.emptyList(); }
        @Override public List<Appointment> findByParentId(Long pId) { return Collections.emptyList(); }
        @Override public boolean updateStatus(Long id, AppointmentStatus status) { return true; }
    }

    private static class InMemoryDoctorDao implements DoctorDao {
        private final Map<Long, Doctor> map = new HashMap<>();
        @Override public Doctor save(Doctor e) { map.put(e.getId(), e); return e; }
        @Override public Optional<Doctor> findById(Long id) { return Optional.ofNullable(map.get(id)); }
        @Override public List<Doctor> findAll() { return new ArrayList<>(map.values()); }
        @Override public boolean deleteById(Long id) { return map.remove(id) != null; }
        @Override public List<Doctor> findBySpecialty(String s) { return Collections.emptyList(); }
        @Override public Optional<Doctor> findByEmail(String email) { return Optional.empty(); }
    }

    private static class InMemoryPatientDao implements PatientDao {
        private final Map<Long, Patient> map = new HashMap<>();
        @Override public Patient save(Patient e) { map.put(e.getId(), e); return e; }
        @Override public Optional<Patient> findById(Long id) { return Optional.ofNullable(map.get(id)); }
        @Override public List<Patient> findAll() { return new ArrayList<>(map.values()); }
        @Override public boolean deleteById(Long id) { return map.remove(id) != null; }
        @Override public List<Patient> searchByName(String name) { return Collections.emptyList(); }
        @Override public Optional<Patient> findByEmail(String email) { return Optional.empty(); }
    }
}