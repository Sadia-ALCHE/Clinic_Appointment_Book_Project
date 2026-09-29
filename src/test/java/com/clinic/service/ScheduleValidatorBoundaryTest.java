package com.clinic.service;

import com.clinic.dao.AppointmentDao;
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
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

// Automated unit test suite verifying ScheduleValidator boundary conditions and conflict detection
public class ScheduleValidatorBoundaryTest {

    private InMemoryAppointmentDao appointmentDao;
    private ScheduleValidator validator;

    @BeforeEach
    public void setUp() {
        appointmentDao = new InMemoryAppointmentDao();
        validator = new ScheduleValidator(appointmentDao);
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
        Appointment appt = new Appointment(null, 1L, 10L, time, "Opening Consult");

        ValidationResult result = validator.validateBooking(appt);

        assertTrue(result.isValid(), "Slot at 08:00 should be valid on a weekday");
    }

    @Test
    @DisplayName("BVA: Should reject booking before clinic opening at 07:30")
    public void shouldRejectBookingBeforeOpeningTime() {
        LocalDate weekday = getNextWeekday();
        LocalDateTime time = LocalDateTime.of(weekday, LocalTime.of(7, 30));
        Appointment appt = new Appointment(null, 1L, 10L, time, "Early Consult");

        ValidationResult result = validator.validateBooking(appt);

        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("08:00"));
    }

    @Test
    @DisplayName("BVA: Should accept last allowable 30-minute booking at 16:30")
    public void shouldAcceptLastBookingAt1630() {
        LocalDate weekday = getNextWeekday();
        LocalDateTime time = LocalDateTime.of(weekday, LocalTime.of(16, 30));
        Appointment appt = new Appointment(null, 1L, 10L, time, "Last Consult");

        ValidationResult result = validator.validateBooking(appt);

        assertTrue(result.isValid(), "Slot at 16:30 concludes at 17:00 and must be accepted");
    }

    @Test
    @DisplayName("BVA: Should reject booking starting at 17:00 as it runs past clinic closing")
    public void shouldRejectBookingAtClosingTime() {
        LocalDate weekday = getNextWeekday();
        LocalDateTime time = LocalDateTime.of(weekday, LocalTime.of(17, 0));
        Appointment appt = new Appointment(null, 1L, 10L, time, "Closing Consult");

        ValidationResult result = validator.validateBooking(appt);

        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("17:00"));
    }

    @Test
    @DisplayName("Should reject booking attempted on weekend days (Saturday or Sunday)")
    public void shouldRejectBookingOnWeekends() {
        LocalDate saturday = LocalDate.now().plusDays(1);
        while (saturday.getDayOfWeek() != DayOfWeek.SATURDAY) {
            saturday = saturday.plusDays(1);
        }
        LocalDateTime weekendTime = LocalDateTime.of(saturday, LocalTime.of(10, 0));
        Appointment appt = new Appointment(null, 1L, 10L, weekendTime, "Weekend Consult");

        ValidationResult result = validator.validateBooking(appt);

        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("Monday through Friday"));
    }

    @Test
    @DisplayName("Should detect doctor overlapping appointment and reject booking")
    public void shouldDetectDoctorOverlappingAppointment() {
        LocalDate weekday = getNextWeekday();
        LocalDateTime bookedTime = LocalDateTime.of(weekday, LocalTime.of(10, 0));

        // Existing appointment for Doctor 10 from 10:00 to 10:30
        appointmentDao.save(new Appointment(100L, 1L, 10L, bookedTime,
                "Initial Consult", AppointmentStatus.CONFIRMED, null, AppointmentType.STANDARD_CONSULTATION));

        // Attempt new booking for Doctor 10 at 10:00 with another patient
        Appointment clash = new Appointment(null, 2L, 10L, bookedTime, "Conflict Consult");
        ValidationResult result = validator.validateBooking(clash);

        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("already booked"));
    }

    @Test
    @DisplayName("Should detect patient concurrent appointment conflict across different doctors")
    public void shouldDetectPatientConcurrentAppointmentConflict() {
        LocalDate weekday = getNextWeekday();
        LocalDateTime bookedTime = LocalDateTime.of(weekday, LocalTime.of(14, 0));

        // Patient 1 already booked with Doctor 10 at 14:00
        appointmentDao.save(new Appointment(101L, 1L, 10L, bookedTime,
                "Triage", AppointmentStatus.CONFIRMED, null, AppointmentType.STANDARD_CONSULTATION));

        // Attempt new booking for same Patient 1 with Doctor 20 at 14:00
        Appointment clash = new Appointment(null, 1L, 20L, bookedTime, "Concurrent Consult");
        ValidationResult result = validator.validateBooking(clash);

        assertFalse(result.isValid());
        // Line 139 in ScheduleValidatorBoundaryTest.java:
        assertTrue(result.getErrorMessage().contains("overlapping"));
    }

    // In-Memory AppointmentDao test double for boundary checks
    private static class InMemoryAppointmentDao implements AppointmentDao {
        private final Map<Long, Appointment> store = new HashMap<>();
        private long seq = 1000L;

        @Override public Appointment save(Appointment e) {
            Long id = e.getId() != null ? e.getId() : seq++;
            Appointment copy = new Appointment(id, e.getPatientId(), e.getDoctorId(), e.getAppointmentDateTime(),
                    e.getReason(), e.getStatus(), e.getParentAppointmentId(), e.getType());
            store.put(id, copy);
            return copy;
        }
        @Override public Optional<Appointment> findById(Long id) { return Optional.ofNullable(store.get(id)); }
        @Override public List<Appointment> findAll() { return new ArrayList<>(store.values()); }
        @Override public boolean deleteById(Long id) { return store.remove(id) != null; }
        @Override public boolean update(Appointment e) { store.put(e.getId(), e); return true; }
        @Override public List<Appointment> findByPatientId(Long pId) {
            return store.values().stream().filter(a -> Objects.equals(a.getPatientId(), pId)).toList();
        }
        @Override public List<Appointment> findByDoctorId(Long dId) {
            return store.values().stream().filter(a -> Objects.equals(a.getDoctorId(), dId)).toList();
        }
        @Override public List<Appointment> findByParentAppointmentId(Long pId) {
            return store.values().stream().filter(a -> Objects.equals(a.getParentAppointmentId(), pId)).toList();
        }
        @Override public List<Appointment> findByDate(LocalDate date) {
            return store.values().stream().filter(a -> a.getAppointmentDateTime().toLocalDate().equals(date)).toList();
        }
        @Override public boolean updateStatus(Long id, AppointmentStatus status) { return true; }
    }
}