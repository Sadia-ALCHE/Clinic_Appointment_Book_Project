package com.clinic.service;

import com.clinic.dao.AppointmentDao;
import com.clinic.dao.DoctorDao;
import com.clinic.model.Appointment;
import com.clinic.model.AppointmentStatus;
import com.clinic.model.AppointmentType;
import com.clinic.model.Doctor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

// Automated stress test suite verifying AppointmentService bounded recursion invariants
public class RecursiveCareChainStressTest {

    private InMemoryAppointmentDao appointmentDao;
    private InMemoryDoctorDao doctorDao;
    private AppointmentService appointmentService;

    @BeforeEach
    public void setUp() {
        appointmentDao = new InMemoryAppointmentDao();
        doctorDao = new InMemoryDoctorDao();
        appointmentService = new AppointmentService(appointmentDao, doctorDao);

        // Seed Doctor with 1,500.0 MUR consultation rate
        doctorDao.save(new Doctor(1L, "Sarah", "Mensah", "General Medicine", 1500.0, "sm@medicare.mu", "+230 5842 1001"));
    }

    @Test
    @DisplayName("Recursion: Single root visit with zero follow-ups has depth 1 and single fee")
    public void shouldCalculateSingleVisitCostAccurately() {
        appointmentDao.save(new Appointment(10L, 1L, 1L, LocalDateTime.now(),
                "Root Visit", AppointmentStatus.COMPLETED, null, AppointmentType.STANDARD_CONSULTATION));

        CareChainSummary summary = appointmentService.summarizeCareChain(10L);

        assertEquals(1, summary.getTotalVisits());
        assertEquals(1, summary.getMaxChainDepth());
        assertEquals(1500.0, summary.getTotalCostMur());
    }

    @Test
    @DisplayName("Recursion: Should accurately calculate 3-visit care chain cumulative cost in MUR")
    public void shouldCalculateMultiVisitCareChainTotalInMur() {
        // Visit 1: Root (1,500.0 MUR)
        appointmentDao.save(new Appointment(10L, 1L, 1L, LocalDateTime.now(),
                "Anchor Consultation", AppointmentStatus.COMPLETED, null, AppointmentType.STANDARD_CONSULTATION));

        // Visit 2: Follow-up child of Visit 1 (1,500.0 MUR)
        appointmentDao.save(new Appointment(20L, 1L, 1L, LocalDateTime.now().plusDays(3),
                "Biopsy Follow-up", AppointmentStatus.CONFIRMED, 10L, AppointmentType.FOLLOW_UP));

        // Visit 3: Follow-up child of Visit 2 (1,500.0 MUR)
        appointmentDao.save(new Appointment(30L, 1L, 1L, LocalDateTime.now().plusDays(7),
                "Discharge Review", AppointmentStatus.CONFIRMED, 20L, AppointmentType.FOLLOW_UP));

        CareChainSummary summary = appointmentService.summarizeCareChain(10L);

        assertEquals(3, summary.getTotalVisits());
        assertEquals(3, summary.getMaxChainDepth());
        assertEquals(4500.0, summary.getTotalCostMur()); // 3 * 1500.0 = 4,500.0 MUR
    }

    @Test
    @DisplayName("Recursion BVA: Should succeed at exact maximum allowable depth of 6")
    public void shouldSucceedAtMaximumAllowableDepthOfSix() {
        Long parentId = null;
        for (long i = 1; i <= 6; i++) {
            appointmentDao.save(new Appointment(i, 1L, 1L, LocalDateTime.now().plusDays(i),
                    "Step " + i, AppointmentStatus.CONFIRMED, parentId, AppointmentType.FOLLOW_UP));
            parentId = i;
        }

        CareChainSummary summary = appointmentService.summarizeCareChain(1L);

        assertEquals(6, summary.getTotalVisits());
        assertEquals(6, summary.getMaxChainDepth());
        assertEquals(9000.0, summary.getTotalCostMur()); // 6 * 1500.0 = 9,000.0 MUR
    }

    @Test
    @DisplayName("Recursion BVA: Should throw IllegalStateException when depth exceeds bound of 6")
    public void shouldThrowIllegalStateExceptionWhenDepthExceedsSix() {
        Long parentId = null;
        for (long i = 1; i <= 7; i++) {
            appointmentDao.save(new Appointment(i, 1L, 1L, LocalDateTime.now().plusDays(i),
                    "Step " + i, AppointmentStatus.CONFIRMED, parentId, AppointmentType.FOLLOW_UP));
            parentId = i;
        }

        assertThrows(IllegalStateException.class, () ->
                appointmentService.summarizeCareChain(1L));
    }

    @Test
    @DisplayName("Recursion: Should detect circular appointment pointers and throw IllegalStateException")
    public void shouldDetectCircularParentPointersAndThrowException() {
        // Appt 1 points to parent 2, and Appt 2 points to parent 1 (Cycle)
        appointmentDao.save(new Appointment(100L, 1L, 1L, LocalDateTime.now(),
                "Visit A", AppointmentStatus.CONFIRMED, 200L, AppointmentType.FOLLOW_UP));
        appointmentDao.save(new Appointment(200L, 1L, 1L, LocalDateTime.now().plusDays(1),
                "Visit B", AppointmentStatus.CONFIRMED, 100L, AppointmentType.FOLLOW_UP));

        assertThrows(IllegalStateException.class, () ->
                appointmentService.summarizeCareChain(100L));
    }

    // Lightweight In-Memory DAO test doubles
    private static class InMemoryAppointmentDao implements AppointmentDao {
        private final Map<Long, Appointment> store = new HashMap<>();

        @Override public Appointment save(Appointment e) { store.put(e.getId(), e); return e; }
        @Override public Optional<Appointment> findById(Long id) { return Optional.ofNullable(store.get(id)); }
        @Override public List<Appointment> findAll() { return new ArrayList<>(store.values()); }
        @Override public boolean deleteById(Long id) { return store.remove(id) != null; }
        @Override public boolean update(Appointment e) { store.put(e.getId(), e); return true; }
        @Override public List<Appointment> findByDoctorId(Long dId) { return Collections.emptyList(); }
        @Override public List<Appointment> findByPatientId(Long pId) { return Collections.emptyList(); }
        @Override public List<Appointment> findByParentAppointmentId(Long parentId) {
            List<Appointment> res = new ArrayList<>();
            for (Appointment a : store.values()) {
                if (parentId != null && parentId.equals(a.getParentAppointmentId())) {
                    res.add(a);
                }
            }
            return res;
        }
        @Override public List<Appointment> findByDate(LocalDate date) { return Collections.emptyList(); }
        @Override public boolean updateStatus(Long id, AppointmentStatus status) { return true; }
    }

    private static class InMemoryDoctorDao implements DoctorDao {
        private final Map<Long, Doctor> store = new HashMap<>();
        @Override public Doctor save(Doctor e) { store.put(e.getId(), e); return e; }
        @Override public Optional<Doctor> findById(Long id) { return Optional.ofNullable(store.get(id)); }
        @Override public List<Doctor> findAll() { return new ArrayList<>(store.values()); }
        @Override public boolean deleteById(Long id) { return store.remove(id) != null; }
        @Override public boolean update(Doctor e) { store.put(e.getId(), e); return true; }
        @Override public List<Doctor> findBySpecialty(String s) { return Collections.emptyList(); }
        @Override public boolean updateHourlyRate(Long id, double newRateMur) { return true; }
    }
}