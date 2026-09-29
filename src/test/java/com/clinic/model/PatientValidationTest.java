package com.clinic.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

// Automated unit test suite verifying Patient entity validation rules and Mauritian data standards
public class PatientValidationTest {

    @Test
    @DisplayName("Should accept valid Mauritian patient with campus credentials")
    public void shouldAcceptValidMauritianPatientDetails() {
        Patient patient = new Patient(1L, "Adebayo Ogunlesi", "adebayo@alche.edu.mu", "+230 5842 1099", LocalDate.of(2003, 5, 14));

        assertNotNull(patient);
        assertEquals("Adebayo Ogunlesi", patient.getFullName());
        assertEquals("adebayo@alche.edu.mu", patient.getEmail());
        assertEquals("+230 5842 1099", patient.getPhoneNumber());
        assertEquals(LocalDate.of(2003, 5, 14), patient.getDateOfBirth());
    }

    @Test
    @DisplayName("Should reject patient construction with null or blank full name")
    public void shouldRejectBlankOrNullPatientName() {
        assertThrows(IllegalArgumentException.class, () ->
                new Patient(1L, "   ", "adebayo@alche.edu.mu", "+230 5842 1099", LocalDate.of(2003, 5, 14)));

        assertThrows(IllegalArgumentException.class, () ->
                new Patient(1L, null, "adebayo@alche.edu.mu", "+230 5842 1099", LocalDate.of(2003, 5, 14)));
    }

    @Test
    @DisplayName("Should reject patient construction with malformed email address")
    public void shouldRejectInvalidEmailFormat() {
        assertThrows(IllegalArgumentException.class, () ->
                new Patient(1L, "Adebayo Ogunlesi", "invalid-email-address", "+230 5842 1099", LocalDate.of(2003, 5, 14)));

        assertThrows(IllegalArgumentException.class, () ->
                new Patient(1L, "Adebayo Ogunlesi", "adebayo@", "+230 5842 1099", LocalDate.of(2003, 5, 14)));
    }

    @Test
    @DisplayName("Should accept authentic 8-digit Mauritian phone numbers with country code")
    public void shouldAcceptMauritianPhoneNumberFormats() {
        Patient p1 = new Patient(1L, "Adebayo Ogunlesi", "adebayo@alche.edu.mu", "+230 5842 1099", LocalDate.of(2003, 5, 14));
        Patient p2 = new Patient(2L, "Fatima Bello", "fatima@alche.edu.mu", "58421099", LocalDate.of(2002, 3, 10));

        assertTrue(p1.getPhoneNumber().contains("5842"));
        assertTrue(p2.getPhoneNumber().contains("5842"));
    }

    @Test
    @DisplayName("Should reject non-Mauritian or invalid phone numbers")
    public void shouldRejectInvalidPhoneNumber() {
        assertThrows(IllegalArgumentException.class, () ->
                new Patient(1L, "Adebayo Ogunlesi", "adebayo@alche.edu.mu", "1234", LocalDate.of(2003, 5, 14)));

        assertThrows(IllegalArgumentException.class, () ->
                new Patient(1L, "Adebayo Ogunlesi", "adebayo@alche.edu.mu", null, LocalDate.of(2003, 5, 14)));
    }

    @Test
    @DisplayName("Should reject date of birth set in the future")
    public void shouldRejectFutureDateOfBirth() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        assertThrows(IllegalArgumentException.class, () ->
                new Patient(1L, "Adebayo Ogunlesi", "adebayo@alche.edu.mu", "+230 5842 1099", tomorrow));
    }
}
