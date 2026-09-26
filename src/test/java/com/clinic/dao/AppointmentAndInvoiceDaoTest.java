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
}