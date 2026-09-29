package com.clinic.service;

import com.clinic.dao.AppointmentDao;
import com.clinic.model.Appointment;
import com.clinic.model.AppointmentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

// Automated unit test suite verifying ScheduleValidator business logic and boundary invariants
// Tests operating hours, interval collision math, cancelled slot reuse, and follow-up chronology
public class ScheduleValidatorTest {

    // Test double: in-memory implementation of AppointmentDao for blistering fast unit tests
    private static class InMemoryAppointmentDao implements AppointmentDao {
        private final List<Appointment> appointments = new ArrayList<>();
        private long idSequence = 100L;

        @Override
        public Appointment save(Appointment appt) {
            Long assignedId = appt.getId() != null ? appt.getId() : ++idSequence;
            Appointment copy = new Appointment(
                    assignedId,
                    appt.getPatientId(),
                    appt.getDoctorId(),
                    appt.getAppointmentDateTime(),
                    appt.getReason(),
                    appt.getStatus(),
                    appt.getParentAppointmentId()
            );
            appointments.add(copy);
            return copy;
        }

        @Override
        public Optional<Appointment> findById(Long id) {
            return appointments.stream().filter(a -> Objects.equals(a.getId(), id)).findFirst();
        }

        @Override
        public List<Appointment> findAll() { return new ArrayList<>(appointments); }

        @Override
        public boolean update(Appointment appt) {
            deleteById(appt.getId());
            appointments.add(appt);
            return true;
        }

        @Override
        public boolean deleteById(Long id) {
            return appointments.removeIf(a -> Objects.equals(a.getId(), id));
        }

        @Override
        public List<Appointment> findByPatientId(Long patientId) {
            return appointments.stream().filter(a -> Objects.equals(a.getPatientId(), patientId)).toList();
        }

        @Override
        public List<Appointment> findByDoctorId(Long doctorId) {
            return appointments.stream().filter(a -> Objects.equals(a.getDoctorId(), doctorId)).toList();
        }

        @Override
        public List<Appointment> findByParentAppointmentId(Long parentId) {
            return appointments.stream().filter(a -> Objects.equals(a.getParentAppointmentId(), parentId)).toList();
        }

        @Override
        public List<Appointment> findByDate(LocalDate date) {
            return appointments.stream().filter(a -> a.getAppointmentDateTime().toLocalDate().isEqual(date)).toList();
        }

        @Override
        public boolean updateStatus(Long id, AppointmentStatus newStatus) {
            Optional<Appointment> opt = findById(id);
            if (opt.isPresent()) {
                opt.get().setStatus(newStatus);
                return true;
            }
            return false;
        }
    }

    private InMemoryAppointmentDao appointmentDao;
    private ScheduleValidator validator;

    // Freeze test time to Tuesday, 15 September 2026, 08:00 AM UTC
    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-09-15T08:00:00Z"),
            ZoneId.of("UTC")
    );

    @BeforeEach
    void setUp() {
        appointmentDao = new InMemoryAppointmentDao();
        validator = new ScheduleValidator(appointmentDao, FIXED_CLOCK);
    }

    @Test
    @DisplayName("Valid appointment on a weekday during operating hours should succeed")
    void testValidBookingSucceeds() {
        // Tuesday, 15 Sep 2026 at 10:00 AM
        LocalDateTime time = LocalDateTime.of(2026, 9, 15, 10, 0);
        Appointment appt = new Appointment(null, 1L, 1L, time, "Routine Checkup");

        ValidationResult result = validator.validateBooking(appt);
        assertTrue(result.isValid(), "Valid booking request should be approved");
        assertNull(result.getErrorMessage());
    }

    @Test
    @DisplayName("Booking on Saturday or Sunday should be rejected with weekend alert")
    void testWeekendBookingFails() {
        // Saturday, 19 Sep 2026 at 10:00 AM
        LocalDateTime saturday = LocalDateTime.of(2026, 9, 19, 10, 0);
        Appointment appt = new Appointment(null, 1L, 1L, saturday, "Saturday Checkup");

        ValidationResult result = validator.validateBooking(appt);
        assertFalse(result.isValid(), "Weekend booking must be rejected");
        assertTrue(result.getErrorMessage().contains("closed on weekends"));
    }

    @Test
    @DisplayName("Booking starting before 08:00 opening time should be rejected")
    void testEarlyMorningBookingFails() {
        // Tuesday at 07:30 AM
        LocalDateTime early = LocalDateTime.of(2026, 9, 15, 7, 30);
        Appointment appt = new Appointment(null, 1L, 1L, early, "Early Morning");

        ValidationResult result = validator.validateBooking(appt);
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("opening time"));
    }

    @Test
    @DisplayName("Booking ending after 17:00 closing time should be rejected")
    void testLateEveningBookingFails() {
        // Tuesday at 16:45 for 30 minutes (concludes at 17:15, after 17:00)
        LocalDateTime late = LocalDateTime.of(2026, 9, 15, 16, 45);
        Appointment appt = new Appointment(null, 1L, 1L, late, "Late Evening");

        ValidationResult result = validator.validateBooking(appt, 30);
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("closing time"));
    }

    @Test
    @DisplayName("Booking in the past should be rejected")
    void testPastDateTimeBookingFails() {
        // Monday, 14 Sep 2026 (one day before our fixed clock of 15 Sep 2026)
        LocalDateTime past = LocalDateTime.of(2026, 9, 14, 10, 0);
        Appointment appt = new Appointment(null, 1L, 1L, past, "Past Consultation");

        ValidationResult result = validator.validateBooking(appt);
        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().contains("in the past"));
    }

    @Test
    @DisplayName("Doctor double-booking with exact overlap should be rejected")
    void testDoctorDoubleBookingCollision() {
        LocalDateTime time = LocalDateTime.of(2026, 9, 15, 10, 0);

        // Pre-existing booking for Doctor 1 with Patient 1
        appointmentDao.save(new Appointment(null, 1L, 1L, time, "Existing Booking", AppointmentStatus.SCHEDULED, null));

        // New booking attempt for Doctor 1 with Patient 2 at the exact same time
        Appointment conflicting = new Appointment(null, 2L, 1L, time, "Conflicting Booking");

        ValidationResult result = validator.validateBooking(conflicting);
        assertFalse(result.isValid(), "Doctor double booking must be rejected");
        assertTrue(result.getErrorMessage().contains("Doctor is already booked"));
    }

    @Test
    @DisplayName("Adjacent back-to-back appointments should succeed (half-open interval math)")
    void testDoctorAdjacentBackToBackSlotsSucceeds() {
        LocalDateTime slot1 = LocalDateTime.of(2026, 9, 15, 10, 0);
        LocalDateTime slot2 = LocalDateTime.of(2026, 9, 15, 10, 30); // Exactly adjacent

        // Existing booking 10:00 - 10:30
        appointmentDao.save(new Appointment(null, 1L, 1L, slot1, "Booking 1", AppointmentStatus.SCHEDULED, null));

        // New booking 10:30 - 11:00 for the same doctor
        Appointment adjacent = new Appointment(null, 2L, 1L, slot2, "Adjacent Booking");

        ValidationResult result = validator.validateBooking(adjacent, 30);
        assertTrue(result.isValid(), "Adjacent back-to-back appointment must succeed without conflict");
    }

    @Test
    @DisplayName("Cancelled appointment should release time slot for new bookings")
    void testCancelledAppointmentDoesNotBlockSlot() {
        LocalDateTime time = LocalDateTime.of(2026, 9, 15, 11, 0);

        // Saved as CANCELLED
        appointmentDao.save(new Appointment(null, 1L, 1L, time, "Cancelled Visit", AppointmentStatus.CANCELLED, null));

        // New booking at the vacated slot
        Appointment newAppt = new Appointment(null, 2L, 1L, time, "New Visit");

        ValidationResult result = validator.validateBooking(newAppt);
        assertTrue(result.isValid(), "Cancelled appointment must not block newly scheduled visits");
    }

    @Test
    @DisplayName("Rescheduling an existing appointment should not collide with itself")
    void testReschedulingExcludesSelf() {
        LocalDateTime time = LocalDateTime.of(2026, 9, 15, 14, 0);
        Appointment saved = appointmentDao.save(new Appointment(null, 1L, 1L, time, "Original Visit", AppointmentStatus.SCHEDULED, null));

        // Attempt to update the same appointment (same ID) at the same time
        Appointment updated = new Appointment(saved.getId(), 1L, 1L, time, "Updated Visit", AppointmentStatus.CONFIRMED, null);

        ValidationResult result = validator.validateBooking(updated);
        assertTrue(result.isValid(), "Rescheduling an appointment should exclude itself from conflict detection");
    }

    @Test
    @DisplayName("Patient cannot have two concurrent appointments with different doctors")
    void testPatientConcurrentBookingFails() {
        LocalDateTime time = LocalDateTime.of(2026, 9, 15, 15, 0);

        // Patient 1 already booked with Doctor 1
        appointmentDao.save(new Appointment(null, 1L, 1L, time, "Dentistry", AppointmentStatus.SCHEDULED, null));

        // Patient 1 attempts booking with Doctor 2 at the same time
        Appointment conflicting = new Appointment(null, 1L, 2L, time, "Cardiology");

        ValidationResult result = validator.validateBooking(conflicting);
        assertFalse(result.isValid(), "Patient cannot be double-booked across different doctors");
        assertTrue(result.getErrorMessage().contains("Patient already has an active overlapping appointment"));
    }

    @Test
    @DisplayName("Follow-up appointment scheduled before parent consultation should be rejected")
    void testFollowUpBeforeParentFails() {
        LocalDateTime parentTime = LocalDateTime.of(2026, 9, 16, 10, 0);
        Appointment parent = appointmentDao.save(new Appointment(null, 1L, 1L, parentTime, "Initial Visit", AppointmentStatus.SCHEDULED, null));

        // Follow-up attempted on 15 Sep (before parent visit on 16 Sep)
        LocalDateTime invalidFollowUpTime = LocalDateTime.of(2026, 9, 15, 10, 0);
        Appointment followUp = new Appointment(null, 1L, 1L, invalidFollowUpTime, "Premature Follow-up", AppointmentStatus.SCHEDULED, parent.getId());

        ValidationResult result = validator.validateBooking(followUp);
        assertFalse(result.isValid(), "Follow-up must occur after parent appointment");
        assertTrue(result.getErrorMessage().contains("must be scheduled after the parent consultation"));
    }

    @Test
    @DisplayName("Valid follow-up appointment scheduled after parent consultation should succeed")
    void testValidFollowUpAfterParentSucceeds() {
        LocalDateTime parentTime = LocalDateTime.of(2026, 9, 15, 10, 0);
        Appointment parent = appointmentDao.save(new Appointment(null, 1L, 1L, parentTime, "Initial Visit", AppointmentStatus.SCHEDULED, null));

        // Follow-up scheduled 7 days later
        LocalDateTime followUpTime = parentTime.plusDays(7);
        Appointment followUp = new Appointment(null, 1L, 1L, followUpTime, "Post-treatment Review", AppointmentStatus.SCHEDULED, parent.getId());

        ValidationResult result = validator.validateBooking(followUp);
        assertTrue(result.isValid(), "Valid follow-up visit after parent appointment must succeed");
    }
}