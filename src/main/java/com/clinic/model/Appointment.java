package com.clinic.model;

import java.time.LocalDateTime;
import java.util.Objects;

public class Appointment {
    private final Long id;
    private final Long patientId;
    private final Long doctorId;
    private LocalDateTime appointmentDateTime;
    private String reason;
    private AppointmentStatus status;
    private Long parentAppointmentId; //Nullable: references parent consultation if this is a follow-up
}

public Appointment(Long id, Long patientId, Long doctorId, LocalDateTime appointmentDateTime,
                   String reason, AppointmentStatus status, Long parentAppointmentId) {
    if (patientId == null) {
        throw new IllegalArgumentException("Patient ID cannot be null.");
    }
    if (doctorId == null) {
        throw new IllegalArgumentException("Doctor ID cannot be null.");
    }
    if (appointmentDateTime == null) {
        throw new IllegalArgumentException("Appointment date and time cannot be null.");
    }
    if (reason == null || reason.trim().isEmpty()) {
        throw new IllegalArgumentException("Clinical reason for visit cannot be empty.");
    }

    this.id = id;
    this.patientId = patientId;
    this.doctorId = doctorId;
    this.appointmentDateTime = appointmentDateTime;
    this.reason = reason.trim();
    // New appointments default to SCHEDULED when no status is provided.
    this.status = (status != null) ? status : AppointmentStatus.SCHEDULED;
    this.parentAppointmentId = parentAppointmentId;
}