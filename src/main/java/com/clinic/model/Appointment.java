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

    // Overloaded constructor for brand-new root appointments (no parent ID)
    public Appointment(Long id, Long patientId, Long doctorId,
                       LocalDateTime appointmentDateTime, String reason) {
        this(id, patientId, doctorId, appointmentDateTime, reason,
                AppointmentStatus.SCHEDULED, null);
    }

    public Long getId() { return id; }
    public Long getPatientId() { return patientId; }
    public Long getDoctorId() { return doctorId; }
    public LocalDateTime getAppointmentDateTime() { return appointmentDateTime; }
    public String getReason() { return reason; }
    public AppointmentStatus getStatus() { return status; }
    public Long getParentAppointmentId() { return parentAppointmentId; }

    // An appointment is a follow-up when it references a previous appointment.
    public boolean isFollowUp() {
        return this.parentAppointmentId != null;
    }

    public void setStatus(AppointmentStatus newStatus) {
        if (newStatus == null) {
            throw new IllegalArgumentException("Status cannot be null.");
        }
        this.status = newStatus;
    }

    public void setAppointmentDateTime(LocalDateTime newDateTime) {
        if (newDateTime == null) {
            throw new IllegalArgumentException("Appointment date/time cannot be null.");
        }
        this.appointmentDateTime = newDateTime;
    }

    public void setReason(String newReason) {
        if (newReason == null || newReason.trim().isEmpty()) {
            throw new IllegalArgumentException("Reason cannot be empty.");
        }
        this.reason = newReason.trim();
    }

    public void setParentAppointmentId(Long parentAppointmentId) {
        // Prevent an appointment from referencing itself as its parent.
        if (Objects.equals(this.id, parentAppointmentId) && this.id != null) {
            throw new IllegalArgumentException("An appointment cannot be a follow-up of itself.");
        }
        this.parentAppointmentId = parentAppointmentId;
    }
}


