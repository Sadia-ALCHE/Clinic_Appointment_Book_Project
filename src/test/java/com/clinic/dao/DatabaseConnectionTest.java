package com.clinic.dao;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

public class DatabaseConnectionTest {

    // Holds the shared in-memory SQLite database alive across multiple connection calls
    private Connection keepAliveConnection;

    @BeforeEach
    public void setUp() throws SQLException {
        // Use a named in-memory database with shared cache
        DatabaseConnection.setDatabaseUrl("jdbc:sqlite:file:testmem?mode=memory&cache=shared");

        // Open the anchor connection so SQLite retains table state in RAM
        keepAliveConnection = DatabaseConnection.getConnection();

        // Bootstrap schema and seed doctors
        DatabaseConnection.initializeDatabase();
    }

    @AfterEach
    public void tearDown() throws SQLException {
        // Closing the anchor connection causes SQLite to purge the in-memory database
        if (keepAliveConnection != null && !keepAliveConnection.isClosed()) {
            keepAliveConnection.close();
        }

        // Reset to production default database path ('jdbc:sqlite:clinic.db')
        DatabaseConnection.resetToDefaultDatabaseUrl();
    }

    @Test
    @DisplayName("Verify active database connection is open and valid")
    public void testConnectionIsOpenAndValid() throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            assertNotNull(conn, "Database connection should not be null");
            assertFalse(conn.isClosed(), "Connection should be open");
            assertTrue(conn.isValid(2), "Connection should be valid within 2 second timeout");
        }
    }

    @Test
    @DisplayName("Verify SQLite PRAGMA foreign_keys is strictly enabled")
    public void testForeignKeysPragmaIsEnabled() throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            boolean isEnabled = DatabaseConnection.isForeignKeyEnforcementActive(conn);
            assertTrue(isEnabled, "PRAGMA foreign_keys must be active (1) to prevent orphaned records");
        }
    }

    @Test
    @DisplayName("Verify schema tables created and baseline doctors pre-seeded in MUR")
    public void testSchemaTablesAndSeedData() throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            // Verify patients table exists in sqlite_master
            try (ResultSet rs = stmt.executeQuery("SELECT count(*) FROM sqlite_master WHERE type='table' AND name='patients';")) {
                assertTrue(rs.next());
                assertEquals(1, rs.getInt(1), "Table 'patients' must exist in SQLite master catalog");
            }

            // Verify doctors table exists
            try (ResultSet rs = stmt.executeQuery("SELECT count(*) FROM sqlite_master WHERE type='table' AND name='doctors';")) {
                assertTrue(rs.next());
                assertEquals(1, rs.getInt(1), "Table 'doctors' must exist in SQLite master catalog");
            }

            // Verify pre-seeded Mauritian doctors exist with realistic rates
            try (ResultSet rs = stmt.executeQuery("SELECT first_name, last_name, hourly_rate FROM doctors WHERE id = 1;")) {
                assertTrue(rs.next(), "Doctor with ID 1 must be pre-seeded");
                assertEquals("Sarah", rs.getString("first_name"));
                assertEquals("Mensah", rs.getString("last_name"));
                assertEquals(1500.0, rs.getDouble("hourly_rate"), 0.001, "Hourly rate must be MUR 1,500.00");
            }
        }
    }
}