package com.clinic.dao.sqlite;

import com.clinic.dao.DatabaseConnection;
import com.clinic.dao.DoctorDao;
import com.clinic.model.Doctor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// SQLite database implementation for DoctorDao interface
// Enforces Mauritian Rupee bounds (MUR 500.00 to 10,000.00) and specialty searches
public class SqliteDoctorDao implements DoctorDao {

    // Saves a doctor to the database and retrieves the generated ID
    @Override
    public Doctor save(Doctor doctor) {
        if (doctor.getHourlyRate() < 500.0 || doctor.getHourlyRate() > 10000.0) {
            throw new IllegalArgumentException("Doctor hourly rate must be between MUR 500.00 and MUR 10,000.00.");
        }
        String sql = """
            INSERT INTO doctors (first_name, last_name, specialty, hourly_rate, email, phone)
            VALUES (?, ?, ?, ?, ?, ?);
            """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, doctor.getFirstName());
            pstmt.setString(2, doctor.getLastName());
            pstmt.setString(3, doctor.getSpecialty());
            pstmt.setDouble(4, doctor.getHourlyRate());
            pstmt.setString(5, doctor.getEmail());
            pstmt.setString(6, doctor.getPhone());
            pstmt.executeUpdate();
            try (ResultSet keys = pstmt.getGeneratedKeys()) {
                if (keys.next()) {
                    return new Doctor(
                            keys.getLong(1),
                            doctor.getFirstName(),
                            doctor.getLastName(),
                            doctor.getSpecialty(),
                            doctor.getHourlyRate(),
                            doctor.getEmail(),
                            doctor.getPhone()
                    );
                }
            }
            throw new SQLException("Failed to obtain generated ID for doctor.");
        } catch (SQLException e) {
            throw new RuntimeException("Error saving doctor to database", e);
        }
    }

    // Finds a doctor by primary key ID
    @Override
    public Optional<Doctor> findById(Long id) {
        String sql = "SELECT id, first_name, last_name, specialty, hourly_rate, email, phone FROM doctors WHERE id = ?;";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return Optional.of(mapRowToDoctor(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding doctor ID: " + id, e);
        }
        return Optional.empty();
    }

    // Finds all doctors practicing a specific specialty
    @Override
    public List<Doctor> findBySpecialty(String specialty) {
        String sql = "SELECT id, first_name, last_name, specialty, hourly_rate, email, phone FROM doctors WHERE specialty = ? ORDER BY last_name;";
        List<Doctor> doctors = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, specialty);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) doctors.add(mapRowToDoctor(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding doctors by specialty: " + specialty, e);
        }
        return doctors;
    }

    // Retrieves all doctors from the database
    @Override
    public List<Doctor> findAll() {
        String sql = "SELECT id, first_name, last_name, specialty, hourly_rate, email, phone FROM doctors ORDER BY last_name;";
        List<Doctor> doctors = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) doctors.add(mapRowToDoctor(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Error finding all doctors", e);
        }
        return doctors;
    }

    // Updates doctor profile information
    @Override
    public boolean update(Doctor doctor) {
        String sql = """
            UPDATE doctors SET first_name = ?, last_name = ?, specialty = ?, hourly_rate = ?, email = ?, phone = ?
            WHERE id = ?;
            """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, doctor.getFirstName());
            pstmt.setString(2, doctor.getLastName());
            pstmt.setString(3, doctor.getSpecialty());
            pstmt.setDouble(4, doctor.getHourlyRate());
            pstmt.setString(5, doctor.getEmail());
            pstmt.setString(6, doctor.getPhone());
            pstmt.setLong(7, doctor.getId());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Error updating doctor ID: " + doctor.getId(), e);
        }
    }

    // Updates doctor hourly rate in Mauritian Rupees (MUR 500.00 to 10,000.00)
    @Override
    public boolean updateHourlyRate(Long id, double newRateMur) {
        if (newRateMur < 500.0 || newRateMur > 10000.0) {
            throw new IllegalArgumentException("Rate must be between MUR 500.00 and MUR 10,000.00.");
        }
        String sql = "UPDATE doctors SET hourly_rate = ? WHERE id = ?;";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDouble(1, newRateMur);
            pstmt.setLong(2, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Error updating doctor rate", e);
        }
    }

    // Deletes a doctor by primary key ID
    @Override
    public boolean deleteById(Long id) {
        String sql = "DELETE FROM doctors WHERE id = ?;";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Error deleting doctor ID: " + id, e);
        }
    }

    // Helper method to convert a database row into a Doctor entity
    private Doctor mapRowToDoctor(ResultSet rs) throws SQLException {
        return new Doctor(
                rs.getLong("id"),
                rs.getString("first_name"),
                rs.getString("last_name"),
                rs.getString("specialty"),
                rs.getDouble("hourly_rate"),
                rs.getString("email"),
                rs.getString("phone")
        );
    }
}