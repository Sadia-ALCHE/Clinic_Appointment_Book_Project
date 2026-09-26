package com.clinic.dao;

import com.clinic.model.Appointment;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class SqliteAppointmentDao implements AppointmentDao {

    private final DatabaseConnection dbConnection;
    // Keeps appointment date/time values in a consistent format
    // when storing and reading them from SQLite.
    private static final DateTimeFormatter ISO_FORMATTER =
            DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    // Receives the database connection that this DAO will use.
    public SqliteAppointmentDao(DatabaseConnection dbConnection) {
        // The DAO cannot work without a database connection.
        if (dbConnection == null) {
            throw new IllegalArgumentException(
                    "DatabaseConnection cannot be null."
            );
        }

        this.dbConnection = dbConnection;
    }

    @Override
    public Appointment save(Appointment appointment) {
        if (appointment == null) {
            throw new IllegalArgumentException("Appointment cannot be null.");
        }

        // The ? symbols are serving as placeholders for the actual values
        String sql = """
        INSERT INTO appointments
        (patient_id, doctor_id, appointment_datetime, reason, status, parent_appointment_id)
        VALUES (?, ?, ?, ?, ?, ?);
        """;

        try (Connection conn = dbConnection.getConnection();
                // Prepare the SQL statement and ask SQLite to return
                // the ID generated for the new appointment.
                PreparedStatement pstmt =
                        conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
        ) {

            // Fill the SQL placeholders with appointment data.
            pstmt.setLong(1, appointment.getPatientId());
            pstmt.setLong(2, appointment.getDoctorId());

            // Convert LocalDateTime into text before storing it in SQLite.
            pstmt.setString(3, appointment.getAppointmentDateTime().format(ISO_FORMATTER));
            pstmt.setString(4, appointment.getReason());
            // Store the enum as text, e.g. "SCHEDULED".
            pstmt.setString(5, appointment.getStatus().name());

            // parent_appointment_id can be null because not every
            // appointment is a follow-up appointment.
            if (appointment.getParentAppointmentId() != null) {
                pstmt.setLong(6, appointment.getParentAppointmentId());
            } else {
                pstmt.setNull(6, Types.INTEGER);
            }

            // Actually execute the INSERT statement.
            pstmt.executeUpdate();

            // SQLite gives us the ID it generated for the new appointment.
            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    // Get the newly generated appointment ID.
                    long generatedId = generatedKeys.getLong(1);
                    // Return a new Appointment containing the generated ID.
                    return new Appointment(
                            generatedId,
                            appointment.getPatientId(),
                            appointment.getDoctorId(),
                            appointment.getAppointmentDateTime(),
                            appointment.getReason(),
                            appointment.getStatus(),
                            appointment.getParentAppointmentId()
                    );
                } else {
                    throw new SQLException("Failed to capture generated appointment ID.");
                }
            }
        } catch (SQLException e) {
            // Convert the checked SQL exception into a runtime exception
            // with a message that explains what operation failed.
            throw new RuntimeException(
                    "Database error saving appointment: " + e.getMessage(), e);
        }
    }


