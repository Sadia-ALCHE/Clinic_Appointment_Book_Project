package com.clinic.factory;

import com.clinic.model.Appointment;
import com.clinic.model.AppointmentStatus;
import com.clinic.model.AppointmentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

// Automated unit test suite verifying Creational Design Pattern 2: AppointmentFactory
// Validates archetype defaults, parent pointer invariants, and status configurations
public class AppointmentFactoryTest {

    private final Long testPatientId = 101L;
    private final Long testDoctorId = 2L;
    private final LocalDateTime testDateTime = LocalDateTime.of(2026, 9, 15, 10, 0);

    @Test
    @DisplayName("Standard consultation should use SCHEDULED status and no parent")
    void testCreateStandardConsultationDefaults() {
        Appointment appointment = AppointmentFactory.createStandardConsultation(testPatientId, testDoctorId, testDateTime);

        assertNotNull(appointment, "Factory should return an Appointment");
        assertNull(appointment.getId(), "New appointment should not have a database ID yet");
        assertEquals(testPatientId, appointment.getPatientId());
        assertEquals(testDoctorId, appointment.getDoctorId());
        assertEquals(testDateTime, appointment.getAppointmentDateTime());
        // A standard consultation is a new root appointment.
        assertEquals(AppointmentStatus.SCHEDULED, appointment.getStatus());
        assertEquals(AppointmentType.STANDARD_CONSULTATION, appointment.getType());
        assertNull(appointment.getParentAppointmentId());
        assertFalse(appointment.isFollowUp());
        // The factory should apply the standard default reason.
        assertEquals(AppointmentFactory.DEFAULT_STANDARD_REASON, appointment.getReason());
    }

