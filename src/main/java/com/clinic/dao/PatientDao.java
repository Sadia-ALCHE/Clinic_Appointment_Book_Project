package com.clinic.dao;

import com.clinic.model.Patient;
import java.util.List;
import java.util.Optional;

//Data Access Object contract for Patient entities to be able to search patients by name and email address
// Search by name returns list matching either first or last name
// Search by email returns Optional because email is unique in the schema which means it's either found or Not found.
// Optional handles the 'not found' case avoiding NullPointerException
public interface PatientDao extends Dao<Patient, Long> {
    List<Patient> searchByName(String searchTerm);
    Optional<Patient> findByEmail(String email);
}
