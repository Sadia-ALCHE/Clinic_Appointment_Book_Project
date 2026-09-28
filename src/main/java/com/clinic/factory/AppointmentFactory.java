package com.clinic.factory;

import com.clinic.model.Appointment;
import com.clinic.model.AppointmentStatus;
import com.clinic.model.AppointmentType;

import java.time.LocalDateTime;
import java.util.Objects;

// Creates appointments using predefined appointment types and rules.
public final class AppointmentFactory {

    // Default reasons used when creating common appointment types.
    public static final String DEFAULT_STANDARD_REASON = "Standard Outpatient Clinical Consultation";
    public static final String DEFAULT_FOLLOW_UP_REASON = "Post-Treatment Clinical Follow-Up Review";
    public static final String DEFAULT_EMERGENCY_REASON = "Urgent Acute Medical Care / Emergency Triage";

    // Private constructor prevents this utility class from being instantiated.
    private AppointmentFactory() {
        // AppointmentFactory only provides static methods.
    }

    // Creates a standard consultation appointment with custom clinical reason
    public static Appointment createStandardConsultation(Long patientId, Long doctorId,
                                                         LocalDateTime dateTime, String reason) {
        String visitReason = (reason != null && !reason.trim().isEmpty()) ? reason.trim() : DEFAULT_STANDARD_REASON;
        return new Appointment(
                null,
                patientId,
                doctorId,
                dateTime,
                visitReason,
                AppointmentStatus.SCHEDULED,
                null,
                AppointmentType.STANDARD_CONSULTATION
        );
    }

    // Convenience overload creating a standard consultation with default clinical reason
    public static Appointment createStandardConsultation(Long patientId, Long doctorId, LocalDateTime dateTime) {
        return createStandardConsultation(patientId, doctorId, dateTime, DEFAULT_STANDARD_REASON);
    }

    // Creates a follow-up appointment attached to an existing parent consultation
    public static Appointment createFollowUpVisit(Long patientId, Long doctorId, LocalDateTime dateTime,
                                                  Long parentAppointmentId, String reason) {
        // Enforce mandatory parent reference invariant
        if (parentAppointmentId == null) {
            throw new IllegalArgumentException("Parent appointment ID cannot be null for a follow-up visit.");
        }

        String visitReason = (reason != null && !reason.trim().isEmpty()) ? reason.trim() : DEFAULT_FOLLOW_UP_REASON;
        return new Appointment(
                null,
                patientId,
                doctorId,
                dateTime,
                visitReason,
                AppointmentStatus.SCHEDULED,
                parentAppointmentId,
                AppointmentType.FOLLOW_UP
        );
    }

    // Convenience overload for follow-up visit with default clinical reason
    public static Appointment createFollowUpVisit(Long patientId, Long doctorId, LocalDateTime dateTime, Long parentAppointmentId) {
        return createFollowUpVisit(patientId, doctorId, dateTime, parentAppointmentId, DEFAULT_FOLLOW_UP_REASON);
    }

    // Creates an urgent acute emergency checkup with pre-confirmed clinical status
    public static Appointment createEmergencyCheckup(Long patientId, Long doctorId,
                                                     LocalDateTime dateTime, String emergencyDescription) {
        String visitReason = (emergencyDescription != null && !emergencyDescription.trim().isEmpty()) ? "EMERGENCY: " + emergencyDescription.trim() : DEFAULT_EMERGENCY_REASON;
        return new Appointment(
                null,
                patientId,
                doctorId,
                dateTime,
                visitReason,
                AppointmentStatus.CONFIRMED,
                null,
                AppointmentType.EMERGENCY
        );
    }

    // Convenience overload for emergency checkup with default triage description
    public static Appointment createEmergencyCheckup(Long patientId, Long doctorId, LocalDateTime dateTime) {
        return createEmergencyCheckup(patientId, doctorId, dateTime, DEFAULT_EMERGENCY_REASON);
    }
}