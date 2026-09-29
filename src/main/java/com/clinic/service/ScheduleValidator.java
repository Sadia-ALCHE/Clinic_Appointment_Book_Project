package com.clinic.service;

import com.clinic.dao.AppointmentDao;
import com.clinic.model.Appointment;
import com.clinic.model.AppointmentStatus;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

// Business logic validation engine for MediCare clinic appointment scheduling
// Enforces operating hours (08:00 - 17:00, Mon-Fri), detects conflicts between schedules, and follow-up appointments validation
public class ScheduleValidator {

    // Clinic opening time: 08:00 AM
    public static final LocalTime CLINIC_OPEN = LocalTime.of(8, 0);

    // Clinic closing time: 17:00 PM (5:00 PM)
    public static final LocalTime CLINIC_CLOSE = LocalTime.of(17, 0);

    // Standard consultation slot duration in minutes
    public static final int DEFAULT_DURATION_MINUTES = 30;

    // Minimum allowable consultation duration
    public static final int MIN_DURATION_MINUTES = 15;

    // Maximum allowable consultation duration (2 hours)
    public static final int MAX_DURATION_MINUTES = 120;

    // Data access dependency for querying active appointments
    private final AppointmentDao appointmentDao;

    // Clock instance to support time in automated unit tests
    private final Clock clock;

    // Standard production constructor using system clock
    public ScheduleValidator(AppointmentDao appointmentDao) {
        this(appointmentDao, Clock.systemDefaultZone());
    }

    // Overloaded constructor allowing clock injection for unit testing
    public ScheduleValidator(AppointmentDao appointmentDao, Clock clock) {
        this.appointmentDao = Objects.requireNonNull(appointmentDao, "AppointmentDao cannot be null.");
        this.clock = Objects.requireNonNull(clock, "Clock cannot be null.");
    }

    // Validates clinic calendar working days and daily operating hours (08:00 - 17:00)
    public ValidationResult validateOperatingHours(LocalDateTime startDateTime, int durationMinutes) {
        // Guard against null start timestamp
        if (startDateTime == null) {
            return ValidationResult.invalid("Appointment date and time cannot be null.");
        }

        // Validate duration bounds
        if (durationMinutes < MIN_DURATION_MINUTES || durationMinutes > MAX_DURATION_MINUTES) {
            return ValidationResult.invalid("Appointment duration must be between " +
                    MIN_DURATION_MINUTES + " and " + MAX_DURATION_MINUTES + " minutes.");
        }

        // Weekend constraint: clinic is closed on weekends
        DayOfWeek dayOfWeek = startDateTime.getDayOfWeek();
        if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
            return ValidationResult.invalid("The clinic is closed on weekends. Appointments can only be scheduled Monday through Friday.");
        }

        // Opening hours constraint: appointment cannot start before 08:00
        LocalTime startTime = startDateTime.toLocalTime();
        if (startTime.isBefore(CLINIC_OPEN)) {
            return ValidationResult.invalid("Appointment cannot start before clinic opening time (08:00).");
        }

        // Closing hours constraint: appointment cannot end after 17:00
        LocalDateTime endDateTime = startDateTime.plusMinutes(durationMinutes);
        if (!endDateTime.toLocalDate().isEqual(startDateTime.toLocalDate()) || endDateTime.toLocalTime().isAfter(CLINIC_CLOSE)) {
            return ValidationResult.invalid("Appointment concludes after clinic closing time (17:00).");
        }

        // Past timestamp constraint: appointments cannot be booked in the past
        LocalDateTime now = LocalDateTime.now(clock);
        if (startDateTime.isBefore(now)) {
            return ValidationResult.invalid("Cannot schedule an appointment in the past.");
        }

        return ValidationResult.valid();
    }

    // Validates that the requested doctor has no overlapping active appointments
    public ValidationResult validateDoctorAvailability(Long doctorId, LocalDateTime startDateTime,
                                                       int durationMinutes, Long excludeAppointmentId) {
        // Guard against null doctor identifier
        if (doctorId == null) {
            return ValidationResult.invalid("Doctor ID cannot be null.");
        }

        // Calculate proposed interval end time
        LocalDateTime proposedEnd = startDateTime.plusMinutes(durationMinutes);

        // Retrieve existing appointments for the requested doctor
        List<Appointment> doctorBookings = appointmentDao.findByDoctorId(doctorId);

        // Check for interval collisions across all active bookings
        for (Appointment existing : doctorBookings) {
            // Cancelled appointments release their time slot and do not cause conflict
            if (existing.getStatus() == AppointmentStatus.CANCELLED) {
                continue;
            }

            // Exclude self when rescheduling an existing appointment
            if (excludeAppointmentId != null && excludeAppointmentId.equals(existing.getId())) {
                continue;
            }

            // Existing appointment interval boundaries (assuming default 30-min duration)
            LocalDateTime existingStart = existing.getAppointmentDateTime();
            LocalDateTime existingEnd = existingStart.plusMinutes(DEFAULT_DURATION_MINUTES);

            // Interval intersection theorem: startA < endB AND startB < endA
            if (startDateTime.isBefore(existingEnd) && existingStart.isBefore(proposedEnd)) {
                return ValidationResult.invalid("Doctor is already booked for an overlapping appointment at " +
                        existingStart.toLocalTime() + ".");
            }
        }

        return ValidationResult.valid();
    }

    // Validates that the requested patient has no overlapping active appointments
    public ValidationResult validatePatientAvailability(Long patientId, LocalDateTime startDateTime,
                                                        int durationMinutes, Long excludeAppointmentId) {
        // Guard against null patient identifier
        if (patientId == null) {
            return ValidationResult.invalid("Patient ID cannot be null.");
        }

        // calculate proposed interval end time
        LocalDateTime proposedEnd = startDateTime.plusMinutes(durationMinutes);

        // Check existing appointments for the requested patient
        List<Appointment> patientBookings = appointmentDao.findByPatientId(patientId);

        // Check for interval collisions across patient's active appointments
        for (Appointment existing : patientBookings) {
            // Ignore cancelled appointments
            if (existing.getStatus() == AppointmentStatus.CANCELLED) {
                continue;
            }

            // Exclude self when rescheduling an existing booking
            if (excludeAppointmentId != null && excludeAppointmentId.equals(existing.getId())) {
                continue;
            }

            // Existing appointment interval boundaries
            LocalDateTime existingStart = existing.getAppointmentDateTime();
            LocalDateTime existingEnd = existingStart.plusMinutes(DEFAULT_DURATION_MINUTES);

            // Interval intersection test
            if (startDateTime.isBefore(existingEnd) && existingStart.isBefore(proposedEnd)) {
                return ValidationResult.invalid("Patient already has an active overlapping appointment scheduled at " +
                        existingStart.toLocalTime() + ".");
            }
        }

        return ValidationResult.valid();
    }

    // Validates that follow-up visits reference existing active parent visits and only occur after the existing active parent visit.
    public ValidationResult validateFollowUp(Long parentAppointmentId, LocalDateTime followUpDateTime,
                                             Long currentAppointmentId) {
        // Root appointments have no parent ID
        if (parentAppointmentId == null) {
            return ValidationResult.valid();
        }

        // Constraint to enforce that an appointment cannot be a follow-up of itself
        if (currentAppointmentId != null && currentAppointmentId.equals(parentAppointmentId)) {
            return ValidationResult.invalid("An appointment cannot reference itself as a follow-up.");
        }

        // Retrieve parent appointment from persistence store
        Optional<Appointment> parentOpt = appointmentDao.findById(parentAppointmentId);
        if (parentOpt.isEmpty()) {
            return ValidationResult.invalid("Referenced parent appointment #" + parentAppointmentId + " does not exist.");
        }

        Appointment parent = parentOpt.get();

        // Lifecycle constraint: cannot link a follow-up to a cancelled parent consultation
        if (parent.getStatus() == AppointmentStatus.CANCELLED) {
            return ValidationResult.invalid("Cannot schedule a follow-up visit for a cancelled appointment.");
        }

        // Follow-up must occur after parent appointment
        if (!followUpDateTime.isAfter(parent.getAppointmentDateTime())) {
            return ValidationResult.invalid("Follow-up appointment must be scheduled after the parent consultation (" +
                    parent.getAppointmentDateTime() + ").");
        }

        return ValidationResult.valid();
    }
    // Master validation pipeline managing all business rules for an appointment booking request
    public ValidationResult validateBooking(Appointment appointment, int durationMinutes) {
        // Guard 1: Root null check on appointment entity
        if (appointment == null) {
            return ValidationResult.invalid("Appointment cannot be null.");
        }

        // Guard 2: Required relation identifiers and timestamp
        if (appointment.getPatientId() == null) {
            return ValidationResult.invalid("Patient ID cannot be null.");
        }
        if (appointment.getDoctorId() == null) {
            return ValidationResult.invalid("Doctor ID cannot be null.");
        }
        if (appointment.getAppointmentDateTime() == null) {
            return ValidationResult.invalid("Appointment date and time cannot be null.");
        }

        // Guard 3: Operating hours and calendar bounds
        ValidationResult hoursCheck = validateOperatingHours(appointment.getAppointmentDateTime(), durationMinutes);
        if (!hoursCheck.isValid()) {
            return hoursCheck;
        }

        // Guard 4: Doctor schedule availability for time overlap
        ValidationResult doctorCheck = validateDoctorAvailability(
                appointment.getDoctorId(),
                appointment.getAppointmentDateTime(),
                durationMinutes,
                appointment.getId()
        );
        if (!doctorCheck.isValid()) {
            return doctorCheck;
        }

        // Guard 5: Patient schedule availability for concurrent bookings
        ValidationResult patientCheck = validatePatientAvailability(
                appointment.getPatientId(),
                appointment.getAppointmentDateTime(),
                durationMinutes,
                appointment.getId()
        );
        if (!patientCheck.isValid()) {
            return patientCheck;
        }

        // Guard 6: Parent-child follow-up continuity and time follow-up
        ValidationResult followUpCheck = validateFollowUp(
                appointment.getParentAppointmentId(),
                appointment.getAppointmentDateTime(),
                appointment.getId()
        );
        if (!followUpCheck.isValid()) {
            return followUpCheck;
        }

        // All business validation rules satisfied cleanly
        return ValidationResult.valid();
    }

    // Convenience overload defaulting duration to standard 30-minute consultation
    public ValidationResult validateBooking(Appointment appointment) {
        return validateBooking(appointment, DEFAULT_DURATION_MINUTES);
    }
}