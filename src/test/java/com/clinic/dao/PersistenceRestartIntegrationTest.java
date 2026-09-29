package com.clinic.dao;

import com.clinic.dao.sqlite.SqliteAppointmentDao;
import com.clinic.dao.sqlite.SqliteDoctorDao;
import com.clinic.dao.sqlite.SqliteInvoiceDao;
import com.clinic.dao.sqlite.SqlitePatientDao;
import com.clinic.model.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

// Integration test proving real SQLite persistence across simulated JVM application restarts
public class PersistenceRestartIntegrationTest {

    private File tempDbFile;

    @BeforeEach
    public void setUp() throws Exception {
        // Create an isolated temporary SQLite database file
        tempDbFile = File.createTempFile("medicare_test_", ".db");
        tempDbFile.deleteOnExit();

        // Configure DatabaseConnection utility to target this temporary database and bootstrap schema
        DatabaseConnection.setDatabaseUrl("jdbc:sqlite:" + tempDbFile.getAbsolutePath());
        DatabaseConnection.initializeDatabase();
    }

    @AfterEach
    public void tearDown() {
        // Reset connection manager back to default database and delete temp file
        DatabaseConnection.resetToDefaultDatabaseUrl();
        if (tempDbFile != null && tempDbFile.exists()) {
            tempDbFile.delete();
        }
    }

    @Test
    @DisplayName("Should persist patients, appointments, and invoices across simulated application restart")
    public void shouldPersistAllRecordsAcrossApplicationRestart() {
        // --- PHASE 1: Write records before restart ---
        SqlitePatientDao patientDao1 = new SqlitePatientDao();
        SqliteDoctorDao doctorDao1 = new SqliteDoctorDao();
        SqliteAppointmentDao appointmentDao1 = new SqliteAppointmentDao();
        SqliteInvoiceDao invoiceDao1 = new SqliteInvoiceDao();

        Patient patient = patientDao1.save(new Patient(null, "Fatima Bello", "fatima@alche.edu.mu", "+230 5842 1099", LocalDate.of(2002, 3, 10)));
        assertNotNull(patient.getId());

        Doctor doctor = doctorDao1.save(new Doctor(null, "Sarah", "Mensah", "General Medicine", 1500.0, "sm@medicare.mu", "+230 5842 1001"));
        assertNotNull(doctor.getId());

        Appointment appt = appointmentDao1.save(new Appointment(null, patient.getId(), doctor.getId(),
                LocalDateTime.of(2026, 9, 18, 9, 30), "Triage Review", AppointmentStatus.CONFIRMED, null, AppointmentType.STANDARD_CONSULTATION));
        assertNotNull(appt.getId());

        Invoice invoice = new Invoice(null, appt.getId(), "INV-2026-TEST-01", LocalDate.now(), PaymentStatus.PENDING);
        invoice.addItem(new InvoiceItem(null, null, "Standard Consultation Fee", 1500.0));
        invoice.addItem(new InvoiceItem(null, null, "Blood Glucose Panel", 1200.0));
        Invoice savedInvoice = invoiceDao1.save(invoice);
        assertNotNull(savedInvoice.getId());

        // --- PHASE 2: Simulate JVM Shutdown & Restart ---
        // Nullify all DAO references and reconnect to the exact same database file
        patientDao1 = null;
        doctorDao1 = null;
        appointmentDao1 = null;
        invoiceDao1 = null;

        // Reset and reconnect to the exact same database file
        DatabaseConnection.setDatabaseUrl("jdbc:sqlite:" + tempDbFile.getAbsolutePath());
        SqlitePatientDao patientDao2 = new SqlitePatientDao();
        SqliteDoctorDao doctorDao2 = new SqliteDoctorDao();
        SqliteAppointmentDao appointmentDao2 = new SqliteAppointmentDao();
        SqliteInvoiceDao invoiceDao2 = new SqliteInvoiceDao();

        // --- PHASE 3: Read and assert data after restart ---
        Optional<Patient> retrievedPatient = patientDao2.findById(patient.getId());
        assertTrue(retrievedPatient.isPresent());
        assertEquals("Fatima Bello", retrievedPatient.get().getFullName());
        assertEquals("+230 5842 1099", retrievedPatient.get().getPhoneNumber());

        Optional<Doctor> retrievedDoctor = doctorDao2.findById(doctor.getId());
        assertTrue(retrievedDoctor.isPresent());
        assertEquals("Sarah Mensah", retrievedDoctor.get().getFullName());
        assertEquals(1500.0, retrievedDoctor.get().getHourlyRate());

        Optional<Appointment> retrievedAppt = appointmentDao2.findById(appt.getId());
        assertTrue(retrievedAppt.isPresent());
        assertEquals(AppointmentStatus.CONFIRMED, retrievedAppt.get().getStatus());
        assertEquals("Triage Review", retrievedAppt.get().getReason());

        Optional<Invoice> retrievedInvoice = invoiceDao2.findById(savedInvoice.getId());
        assertTrue(retrievedInvoice.isPresent());
        assertEquals("INV-2026-TEST-01", retrievedInvoice.get().getInvoiceNumber());
        assertEquals(2700.0, retrievedInvoice.get().calculateTotalMur()); // 1500 + 1200 MUR
        assertEquals(2, retrievedInvoice.get().getItems().size());
    }
}