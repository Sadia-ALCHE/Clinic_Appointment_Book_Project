package com.clinic.dao;

import com.clinic.model.Appointment;
import com.clinic.model.AppointmentStatus;
import com.clinic.model.Invoice;
import com.clinic.model.InvoiceItem;
import com.clinic.model.Patient;
import com.clinic.model.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class AppointmentAndInvoiceDaoTest {

    private DatabaseConnection dbConnection;
    private PatientDao patientDao;
    private DoctorDao doctorDao;
    private AppointmentDao appointmentDao;
    private InvoiceDao invoiceDao;

    private Long testPatientId;
    private Long testDoctorId;

    // Sets up a fresh test database and DAO objects before each test.
    @BeforeEach
    void setUp() {
        // Use a separate database file so tests do not affect application data.
        String testDbUrl = "jdbc:sqlite:target/test_clinic_day5.db";
        dbConnection = new DatabaseConnection(testDbUrl);
        dbConnection.initializeSchema();

        // Create DAO objects using the same test database connection.
        patientDao = new SqlitePatientDao(dbConnection);
        doctorDao = new SqliteDoctorDao(dbConnection);
        appointmentDao = new SqliteAppointmentDao(dbConnection);
        invoiceDao = new SqliteInvoiceDao(dbConnection);

        // Create a patient that can be reused by the tests.
        Patient patient = patientDao.save(
                new Patient(
                        "Zubair",
                        "Mohammad",
                        "z.mohammad@mediche.mu",
                        "+230 5789 0011",
                        "1994-06-12",
                        "B+"
                )
        );
        testPatientId = patient.getId();

        // Doctor 1 is pre-seeded by the database schema.
        testDoctorId = 1L;
    }

    @Test
    @DisplayName("Verify root appointment and follow-up appointment tree persistence")
    void testFollowUpAppointmentTree() {
        LocalDateTime visitTime =
                LocalDateTime.of(2026, 9, 15, 9, 30);

        // Create the root appointment with no parent appointment.
        Appointment rootAppt = appointmentDao.save(new Appointment(testPatientId, testDoctorId, visitTime, "Initial Fever Consultation", AppointmentStatus.SCHEDULED, null));
        assertNotNull(
                rootAppt.getId(),
                "Root appointment must receive a generated ID"
        );
        assertFalse(rootAppt.isFollowUp(), "Root visit must have parentAppointmentId == null");

        // Create a follow-up appointment linked to the root appointment.
        LocalDateTime followUpTime = visitTime.plusDays(7);
        Appointment followUpAppt = appointmentDao.save(new Appointment(testPatientId, testDoctorId, followUpTime, "Post-treatment Blood Check", AppointmentStatus.SCHEDULED, rootAppt.getId()));
        assertNotNull(followUpAppt.getId());
        assertTrue(followUpAppt.isFollowUp(), "Follow-up visit must have non-null parentAppointmentId");
        assertEquals(rootAppt.getId(), followUpAppt.getParentAppointmentId());

        // Query the database for appointments linked to the root appointment.
        List<Appointment> followUps = appointmentDao.findByParentAppointmentId(rootAppt.getId());
        assertEquals(1, followUps.size(), "Should find exactly 1 child follow-up appointment");
        assertEquals(followUpAppt.getId(), followUps.get(0).getId());
    }