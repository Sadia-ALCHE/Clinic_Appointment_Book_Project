package com.clinic.dao;

import com.clinic.model.Doctor;
import java.util.List;

// Data Access Object contract for Doctor entities
// Extends generic Dao with specialty search and MUR consultation rate updates
public interface DoctorDao extends Dao<Doctor, Long> {
    List<Doctor> findBySpecialty(String specialty);
    boolean updateHourlyRate(Long id, double newRateMur);
}