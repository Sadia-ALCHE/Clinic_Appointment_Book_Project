package com.clinic.dao.sqlite;

import com.clinic.model.Appointment;
import com.clinic.model.AppointmentStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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

    @Override
    public Optional<Appointment> findById(Long id) {
        // If no ID was provided, there is nothing to search for.
        if (id == null) {
            return Optional.empty();
        }

        // SQL query to find one appointment by its ID.
        String sql = "SELECT id, patient_id, doctor_id, appointment_datetime, reason, status, parent_appointment_id FROM appointments WHERE id = ?;";

        try (Connection conn = dbConnection.getConnection();
             // Prepare the SQL query.
             PreparedStatement pstmt = conn.prepareStatement(sql)
        ) {

            // Replace the ? with the appointment ID.
            pstmt.setLong(1, id);
            // Execute the SELECT query.
            try (ResultSet rs = pstmt.executeQuery()) {
                // If a matching appointment was found,
                if (rs.next()) {
                    // Convert the database row into an Appointment object.
                    return Optional.of(mapRowToAppointment(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error finding appointment by ID: " + id, e);
        }
        // No appointment with that ID was found.
        return Optional.empty();
    }

    private Appointment mapRowToAppointment(ResultSet rs) throws SQLException {
        // Read the appointment ID from the database row.
        long id = rs.getLong("id");
        // Read the patient and doctor IDs.
        long patientId = rs.getLong("patient_id");
        long doctorId = rs.getLong("doctor_id");
        // SQLite stores the date/time as text.
        // Convert that text back into a Java LocalDateTime.
        LocalDateTime dateTime = LocalDateTime.parse(rs.getString("appointment_datetime"), ISO_FORMATTER);
        // Read the appointment reason.
        String reason = rs.getString("reason");
        // Convert the stored text, such as "SCHEDULED",back into the AppointmentStatus enum.
        AppointmentStatus status = AppointmentStatus.valueOf(rs.getString("status"));

        Long parentId = null;
        long rawParentId = rs.getLong("parent_appointment_id");

        // getLong() returns 0 when the database value is NULL,so we must check wasNull() to know whether it was actually NULL.
        if (!rs.wasNull()) {
            parentId = rawParentId;
        }

        // Build and return a Java Appointment object from the database.
        return new Appointment(id, patientId, doctorId, dateTime, reason, status, parentId);
    }

    @Override
    public List<Appointment> findByParentAppointmentId(Long parentAppointmentId) {
        // If parentAppointmentId is null, we look for appointments
        // that do NOT have a parent. These are the original appointments.
        String sql = (parentAppointmentId == null)
                ? " SELECT id, patient_id, doctor_id, appointment_datetime, reason, status, parent_appointment_id FROM appointments WHERE parent_appointment_id IS NULL ORDER BY appointment_datetime ASC; "
                : " SELECT id, patient_id, doctor_id, appointment_datetime, reason, status, parent_appointment_id FROM appointments WHERE parent_appointment_id = ? ORDER BY appointment_datetime ASC; ";

        // This list will hold all matching appointments.
        List<Appointment> list = new ArrayList<>();
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // Only add the parameter when we are searching for a specific parent appointment.
            if (parentAppointmentId != null) {
                pstmt.setLong(1, parentAppointmentId);
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                // There may be multiple appointments with the same parent.
                while (rs.next()) {
                    list.add(mapRowToAppointment(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding appointments by parent ID: " + parentAppointmentId, e);
        }
        return list;
    }

    @Override
    public List<Appointment> findByPatientId(Long patientId) {
        // Find all appointments belonging to one patient.
        String sql = "SELECT id, patient_id, doctor_id, appointment_datetime, reason, status, parent_appointment_id FROM appointments WHERE patient_id = ? ORDER BY appointment_datetime DESC;";
        return queryAppointmentList(sql, patientId);
    }

    @Override
    public List<Appointment> findByDoctorId(Long doctorId) {
        // Find all appointments belonging to one doctor.
        String sql = " SELECT id, patient_id, doctor_id, appointment_datetime, reason, status, parent_appointment_id FROM appointments WHERE doctor_id = ? ORDER BY appointment_datetime ASC;";
        return queryAppointmentList(sql, doctorId);
    }

    @Override
    public List<Appointment> findByDate(LocalDate date) {
        // If no date was provided, return an empty list.
        if (date == null)
            return List.of();
        // SQLite extracts the date portion from appointment_datetime.
        String sql = " SELECT id, patient_id, doctor_id, appointment_datetime, reason, status, parent_appointment_id FROM appointments WHERE date(appointment_datetime) = ? ORDER BY appointment_datetime ASC; ";
        List<Appointment> list = new ArrayList<>();

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // LocalDate.toString() produces YYYY-MM-DD,which matches the date format stored in the database.
            pstmt.setString(1, date.toString());
            try (ResultSet rs = pstmt.executeQuery()) {
                // Add every appointment found on that date.
                while (rs.next()) {
                    list.add(mapRowToAppointment(rs));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error querying appointments for date " + date, e);
        }
        return list;
    }

    @Override
    public boolean updateStatus(Long appointmentId, AppointmentStatus newStatus) {
        // Both values are required to perform the update.
        if (appointmentId == null || newStatus == null) return false;
        // Update only the status of the selected appointment.
        String sql = "UPDATE appointments SET status = ? WHERE id = ?; ";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            // Store the enum as text, eg. "COMPLETED".
            pstmt.setString(1, newStatus.name());
            // Identify which appointment should be updated.
            pstmt.setLong(2, appointmentId);
            // executeUpdate() returns the number of rows affected.
            return pstmt.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new RuntimeException("Error updating appointment status: " + appointmentId, e);
        }
    }

    @Override
    public List<Appointment> findAll() {
        // Get every appointment from the database.
        String sql = "SELECT id, patient_id, doctor_id, appointment_datetime, reason, status, parent_appointment_id FROM appointments ORDER BY appointment_datetime DESC; ";
        List<Appointment> list = new ArrayList<>();
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            // Convert every database row into an Appointment object.
            while (rs.next()) {
                list.add(mapRowToAppointment(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error querying all appointments", e);
        }
        return list;
    }


