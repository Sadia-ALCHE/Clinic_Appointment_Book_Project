package com.clinic.dao;

import com.clinic.dao.sqlite.SqlitePatientDao;
import com.clinic.model.Patient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

// Automated integration test suite for PatientDao and SqlitePatientDao
// Verifies CRUD lifecycle, auto-increment key generation, queries, and absence handling
public class PatientDaoTest {

    // Interface reference for polymorphic testing
    private PatientDao patientDao;

    // Initializes database schema once before running the test suite
    // Declares throws SQLException to handle database setup errors
    @BeforeAll
    static void initDatabase() throws SQLException {
        DatabaseConnection.initializeDatabase();
    }

    // Instantiates a fresh DAO implementation before each test executes
    @BeforeEach
    void setUp() {
        patientDao = new SqlitePatientDao();
    }

    // Verifies inserting a patient saves the record and allocates an auto-increment ID
    @Test
    void testSaveAndFindById() {
        // Create a new patient entity without an ID
        Patient newPatient = new Patient(
                null,
                "Amina",
                "Diallo",
                "amina.diallo" + System.currentTimeMillis() + "@test.mu",
                "+230 5789 1234",
                LocalDate.of(1992, 4, 15),
                "O+"
        );

        // Persist patient to database
        Patient saved = patientDao.save(newPatient);

        // Verify SQLite allocated a positive primary key
        assertNotNull(saved.getId(), "Saved patient should have an auto-generated SQLite ID");
        assertTrue(saved.getId() > 0, "Generated ID must be a positive number");

        // Query database by ID to confirm persistent storage
        Optional<Patient> retrieved = patientDao.findById(saved.getId());

        // Verify record is found and matches original attributes
        assertTrue(retrieved.isPresent(), "Patient should be present in database");
        assertEquals("Amina", retrieved.get().getFirstName(), "First name must match");
        assertEquals("Diallo", retrieved.get().getLastName(), "Last name must match");
    }

    // Verifies that querying a non-existent ID safely returns Optional.empty()
    @Test
    void testFindByIdNotFound() {
        // Query an ID that does not exist in the database
        Optional<Patient> absent = patientDao.findById(999999L);

        // Assert absence without throwing NullPointerException
        assertTrue(absent.isEmpty(), "Non-existent patient ID should return Optional.empty()");
    }

    // Verifies searching patients by partial name using SQL LIKE wildcards
    @Test
    void testSearchByName() {
        // Create and save a patient with a unique email to avoid collisions
        String uniqueEmail = "search.test" + System.currentTimeMillis() + "@test.mu";
        Patient p = new Patient(
                null,
                "Kavita",
                "Ramgoolam",
                uniqueEmail,
                "+230 5811 2233",
                LocalDate.of(1988, 10, 20),
                "B+"
        );
        patientDao.save(p);

        // Search by partial last name
        List<Patient> results = patientDao.searchByName("Ramgoolam");

        // Assert results are non-empty and contain the saved patient
        assertFalse(results.isEmpty(), "Search should find patient by last name");
        assertTrue(results.stream().anyMatch(pat -> pat.getEmail().equals(uniqueEmail)),
                "Search results must include the newly created patient");
    }

    // Verifies updating an existing patient record modifies the database row
    @Test
    void testUpdatePatient() {
        // Save initial patient record
        Patient p = new Patient(
                null,
                "Tariq",
                "Mansoor",
                "tariq" + System.currentTimeMillis() + "@test.mu",
                "+230 5999 0000",
                LocalDate.of(1995, 1, 1),
                "A+"
        );
        Patient saved = patientDao.save(p);

        // Create updated entity with new phone number
        Patient updated = new Patient(
                saved.getId(),
                "Tariq",
                "Mansoor",
                saved.getEmail(),
                "+230 5999 8888",
                saved.getDateOfBirth(),
                saved.getBloodGroup()
        );

        // Execute update statement
        boolean success = patientDao.update(updated);
        assertTrue(success, "Update should return true when row is modified");

        // Reload patient from database and verify updated phone number
        Patient reloaded = patientDao.findById(saved.getId()).orElseThrow();
        assertEquals("+230 5999 8888", reloaded.getPhone(), "Reloaded record should reflect new phone number");
    }

    // Verifies deleting a patient removes the row from the database
    @Test
    void testDeleteById() {
        // Save temporary patient for deletion
        Patient p = new Patient(
                null,
                "Temporary",
                "Patient",
                "temp" + System.currentTimeMillis() + "@test.mu",
                "+230 5000 0000",
                LocalDate.of(2000, 1, 1),
                "AB+"
        );
        Patient saved = patientDao.save(p);

        // Execute delete statement
        boolean deleted = patientDao.deleteById(saved.getId());
        assertTrue(deleted, "Delete should return true when row is removed");

        // Confirm patient record no longer exists in database
        Optional<Patient> afterDelete = patientDao.findById(saved.getId());
        assertTrue(afterDelete.isEmpty(), "Patient should no longer exist after deletion");
    }
}