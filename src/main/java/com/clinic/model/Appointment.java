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