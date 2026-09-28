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

    @Test
    @DisplayName("Follow-up visit should use the parent ID and FOLLOW_UP type")
    void testCreateFollowUpVisitValid() {
        Long parentId = 55L;
        Appointment followUp = AppointmentFactory.createFollowUpVisit(testPatientId, testDoctorId, testDateTime, parentId);

        assertNotNull(followUp);
        assertEquals(AppointmentType.FOLLOW_UP, followUp.getType());
        assertEquals(AppointmentStatus.SCHEDULED, followUp.getStatus());
        // A follow-up must point to the earlier appointment it continues.
        assertEquals(parentId, followUp.getParentAppointmentId());
        assertTrue(followUp.isFollowUp());
        assertEquals(AppointmentFactory.DEFAULT_FOLLOW_UP_REASON, followUp.getReason());
    }

    @Test
    @DisplayName("Follow-up visit should reject a null parent ID")
    void testCreateFollowUpMissingParentThrowsException() {
        // A follow-up cannot exist without a parent appointment.
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            AppointmentFactory.createFollowUpVisit(testPatientId, testDoctorId, testDateTime, null);
        });

        assertTrue(ex.getMessage().contains("Parent appointment ID cannot be null"));
    }

    @Test
    @DisplayName("Emergency checkup should use CONFIRMED status and EMERGENCY type")
    void testCreateEmergencyCheckupDefaults() {
        Appointment emergency = AppointmentFactory.createEmergencyCheckup(testPatientId, testDoctorId, testDateTime);

        assertNotNull(emergency);
        // Emergency appointments are created as confirmed root appointments.
        assertEquals(AppointmentType.EMERGENCY, emergency.getType());
        assertEquals(AppointmentStatus.CONFIRMED, emergency.getStatus());
        assertNull(emergency.getParentAppointmentId());
        assertEquals(AppointmentFactory.DEFAULT_EMERGENCY_REASON, emergency.getReason());
    }

