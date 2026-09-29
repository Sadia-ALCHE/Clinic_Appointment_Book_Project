package com.clinic.service;

import com.clinic.model.Appointment;
import com.clinic.model.AppointmentStatus;
import com.clinic.model.AppointmentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// Automated unit test suite verifying ScheduleValidator boundary conditions and conflict detection
public class ScheduleValidatorBoundaryTest {

    private ScheduleValidator validator;
    private List<Appointment> existingAppointments;

    @BeforeEach
    public void setUp() {
        validator = new ScheduleValidator();
        existingAppointments = new ArrayList<>();
    }

    // Helper: Finds next weekday (Monday-Friday) for realistic testing
    private LocalDate getNextWeekday() {
        LocalDate date = LocalDate.now().plusDays(1);
        while (date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            date = date.plusDays(1);
        }
        return date;
    }

    @Test
    @DisplayName("BVA: Should accept booking at exact clinic opening time 08:00")
    public void shouldAcceptBookingAtExactOpeningTime() {
        LocalDate weekday = getNextWeekday();
        LocalDateTime time = LocalDateTime.of(weekday, LocalTime.of(8, 0));

        ValidationResult result = validator.validateBooking(1L, 10L, time, existingAppointments);

        assertTrue(result.isValid(), "Slot at 08:00 should be valid on a weekday");
    }

    @Test
    @DisplayName("BVA: Should reject booking before clinic opening at 07:30")
    public void shouldRejectBookingBeforeOpeningTime() {
        LocalDate weekday = getNextWeekday();
        LocalDateTime time = LocalDateTime.of(weekday, LocalTime.of(7, 30));

        ValidationResult result = validator.validateBooking(1L, 10L, time, existingAppointments);

        assertFalse(result.isValid());
        assertTrue(result.getMessage().contains("Operating hours"));
    }

    @Test
    @DisplayName("BVA: Should accept last allowable 30-minute booking at 16:30")
    public void shouldAcceptLastBookingAt1630() {
        LocalDate weekday = getNextWeekday();
        LocalDateTime time = LocalDateTime.of(weekday, LocalTime.of(16, 30));

        ValidationResult result = validator.validateBooking(1L, 10L, time, existingAppointments);

        assertTrue(result.isValid(), "Slot at 16:30 concludes at 17:00 and must be accepted");
    }

    @Test
    @DisplayName("BVA: Should reject booking starting at 17:00 as it runs past clinic closing")
    public void shouldRejectBookingAtClosingTime() {
        LocalDate weekday = getNextWeekday();
        LocalDateTime time = LocalDateTime.of(weekday, LocalTime.of(17, 0));

        ValidationResult result = validator.validateBooking(1L, 10L, time, existingAppointments);

        assertFalse(result.isValid());
        assertTrue(result.getMessage().contains("Operating hours"));
    }

    @Test
    @DisplayName("Should reject booking attempted on weekend days (Saturday or Sunday)")
    public void shouldRejectBookingOnWeekends() {
        LocalDate saturday = LocalDate.now().plusDays(1);
        while (saturday.getDayOfWeek() != DayOfWeek.SATURDAY) {
            saturday = saturday.plusDays(1);
        }
        LocalDateTime weekendTime = LocalDateTime.of(saturday, LocalTime.of(10, 0));

        ValidationResult result = validator.validateBooking(1L, 10L, weekendTime, existingAppointments);

        assertFalse(result.isValid());
        assertTrue(result.getMessage().contains("Monday through Friday"));
    }

    @Test
    @DisplayName("Should detect doctor overlapping appointment and reject booking")
    public void shouldDetectDoctorOverlappingAppointment() {
        LocalDate weekday = getNextWeekday();
        LocalDateTime bookedTime = LocalDateTime.of(weekday, LocalTime.of(10, 0));

        // Existing appointment for Doctor 1 from 10:00 to 10:30
        existingAppointments.add(new Appointment(100L, 1L, 10L, bookedTime,
                AppointmentStatus.CONFIRMED, "Initial Consult", null, AppointmentType.STANDARD_CONSULTATION));

        // Attempt new booking for Doctor 1 at 10:00 with another patient
        ValidationResult result = validator.validateBooking(2L, 10L, bookedTime, existingAppointments);

        assertFalse(result.isValid());
        assertTrue(result.getMessage().contains("already booked"));
    }

    @Test
    @DisplayName("Should detect patient concurrent appointment conflict across different doctors")
    public void shouldDetectPatientConcurrentAppointmentConflict() {
        LocalDate weekday = getNextWeekday();
        LocalDateTime bookedTime = LocalDateTime.of(weekday, LocalTime.of(14, 0));

        // Patient 1 already booked with Doctor 10 at 14:00
        existingAppointments.add(new Appointment(101L, 1L, 10L, bookedTime,
                AppointmentStatus.CONFIRMED, "Triage", null, AppointmentType.STANDARD_CONSULTATION));

        // Attempt new booking for same Patient 1 with Doctor 20 at 14:00
        ValidationResult result = validator.validateBooking(1L, 20L, bookedTime, existingAppointments);

        assertFalse(result.isValid());
        assertTrue(result.getMessage().contains("concurrent appointment"));
    }
}