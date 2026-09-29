package com.clinic.service;

import com.clinic.dao.AppointmentDao;
import com.clinic.dao.DoctorDao;
import com.clinic.model.Appointment;
import com.clinic.model.AppointmentStatus;
import com.clinic.model.Doctor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

// Automated unit test suite verifying Checkpoint 3 Milestone: Bounded Recursion in AppointmentService
// Validates base cases, tree cascades, depth bounds (<= 6), cycle guards, and MUR cost rollups
public class AppointmentServiceTest {

    // Fast in-memory test double for AppointmentDao
    private static class InMemoryAppointmentDao implements AppointmentDao {
        private final List<Appointment> list = new ArrayList<>();
        private long idGen = 100L;

        @Override
        public Appointment save(Appointment a) {
            Long id = (a.getId() != null) ? a.getId() : ++idGen;
            Appointment copy = new Appointment(id, a.getPatientId(), a.getDoctorId(),
                    a.getAppointmentDateTime(), a.getReason(), a.getStatus(), a.getParentAppointmentId(), a.getType());
            list.add(copy);
            return copy;
        }

        @Override
        public Optional<Appointment> findById(Long id) {
            return list.stream().filter(a -> Objects.equals(a.getId(), id)).findFirst();
        }

        @Override
        public List<Appointment> findAll() { return new ArrayList<>(list); }

        @Override
        public boolean update(Appointment a) {
            deleteById(a.getId());
            list.add(a);
            return true;
        }

        @Override
        public boolean deleteById(Long id) { return list.removeIf(a -> Objects.equals(a.getId(), id)); }

        @Override
        public List<Appointment> findByPatientId(Long pid) {
            return list.stream().filter(a -> Objects.equals(a.getPatientId(), pid)).toList();
        }

        @Override
        public List<Appointment> findByDoctorId(Long did) {
            return list.stream().filter(a -> Objects.equals(a.getDoctorId(), did)).toList();
        }

        @Override
        public List<Appointment> findByParentAppointmentId(Long parentId) {
            return list.stream().filter(a -> Objects.equals(a.getParentAppointmentId(), parentId)).toList();
        }

        @Override
        public List<Appointment> findByDate(LocalDate date) {
            return list.stream().filter(a -> a.getAppointmentDateTime().toLocalDate().isEqual(date)).toList();
        }

        @Override
        public boolean updateStatus(Long id, AppointmentStatus status) {
            Optional<Appointment> opt = findById(id);
            if (opt.isPresent()) { opt.get().setStatus(status); return true; }
            return false;
        }
    }

    // In-memory test double for DoctorDao (Dr. Mensah MUR 1,500.00, Dr. Poisson MUR 2,200.00)
    private static class InMemoryDoctorDao implements DoctorDao {
        @Override
        public Optional<Doctor> findById(Long id) {
            if (Objects.equals(id, 1L)) {
                return Optional.of(new Doctor(1L, "Sarah", "Mensah", "General Practice", 1500.0, "s.mensah@clinic.mu", "+230 5842 1099"));
            } else if (Objects.equals(id, 2L)) {
                return Optional.of(new Doctor(2L, "Jean-Luc", "Poisson", "Cardiology", 2200.0, "jl.poisson@clinic.mu", "+230 5712 3456"));
            }
            return Optional.empty();
        }
        @Override public Doctor save(Doctor d) { return d; }
        @Override public List<Doctor> findAll() { return List.of(); }
        @Override public boolean update(Doctor d) { return true; }
        @Override public boolean deleteById(Long id) { return true; }
        @Override public List<Doctor> findBySpecialty(String specialty) { return List.of(); }
        @Override public boolean updateHourlyRate(Long id, double newRateMur) { return true; }
    }

    private InMemoryAppointmentDao appointmentDao;
    private InMemoryDoctorDao doctorDao;
    private AppointmentService appointmentService;

    @BeforeEach
    void setUp() {
        appointmentDao = new InMemoryAppointmentDao();
        doctorDao = new InMemoryDoctorDao();
        appointmentService = new AppointmentService(appointmentDao, doctorDao);
    }

    @Test
    @DisplayName("Base Case: A single consultation with no follow-ups returns its own consultation fee")
    void testSingleVisitCost() {
        LocalDateTime time = LocalDateTime.of(2026, 9, 15, 10, 0);
        Appointment root = appointmentDao.save(new Appointment(null, 10L, 1L, time, "Initial Visit", AppointmentStatus.SCHEDULED, null));

        double totalCost = appointmentService.calculateTotalChainCost(root.getId());
        assertEquals(1500.0, totalCost, "Single visit with Dr. Mensah must equal MUR 1,500.00");

        CareChainSummary summary = appointmentService.summarizeCareChain(root.getId());
        assertEquals(1, summary.getTotalVisits());
        assertEquals(1, summary.getMaxChainDepth());
        assertEquals(1500.0, summary.getTotalCostMur());
    }

    @Test
    @DisplayName("Two-visit care chain: Root consultation plus one follow-up correctly aggregates both fees")
    void testTwoVisitChainCost() {
        LocalDateTime t1 = LocalDateTime.of(2026, 9, 15, 10, 0);
        Appointment v1 = appointmentDao.save(new Appointment(null, 10L, 1L, t1, "Initial Diagnosis", AppointmentStatus.COMPLETED, null));

        LocalDateTime t2 = t1.plusDays(7);
        Appointment v2 = appointmentDao.save(new Appointment(null, 10L, 2L, t2, "Cardiology Follow-Up", AppointmentStatus.SCHEDULED, v1.getId()));

        // Dr. Mensah (MUR 1500.0) + Dr. Poisson (MUR 2200.0) = MUR 3700.0
        double totalCost = appointmentService.calculateTotalChainCost(v1.getId());
        assertEquals(3700.0, totalCost, "Total chain cost must equal MUR 3,700.00");

        CareChainSummary summary = appointmentService.summarizeCareChain(v1.getId());
        assertEquals(2, summary.getTotalVisits());
        assertEquals(2, summary.getMaxChainDepth());
        assertEquals(3700.0, summary.getTotalCostMur());
    }

    @Test
    @DisplayName("Three-visit linear care cascade correctly traverses from root to leaf")
    void testThreeVisitLinearChainCost() {
        LocalDateTime t1 = LocalDateTime.of(2026, 9, 15, 9, 0);
        Appointment v1 = appointmentDao.save(new Appointment(null, 10L, 1L, t1, "Visit 1", AppointmentStatus.COMPLETED, null));

        LocalDateTime t2 = t1.plusDays(5);
        Appointment v2 = appointmentDao.save(new Appointment(null, 10L, 1L, t2, "Visit 2", AppointmentStatus.COMPLETED, v1.getId()));

        LocalDateTime t3 = t2.plusDays(7);
        Appointment v3 = appointmentDao.save(new Appointment(null, 10L, 1L, t3, "Visit 3", AppointmentStatus.SCHEDULED, v2.getId()));

        // 3 visits with Dr. Mensah: 3 x MUR 1500.0 = MUR 4500.0
        double totalCost = appointmentService.calculateTotalChainCost(v1.getId());
        assertEquals(4500.0, totalCost);

        CareChainSummary summary = appointmentService.summarizeCareChain(v1.getId());
        assertEquals(3, summary.getTotalVisits());
        assertEquals(3, summary.getMaxChainDepth());
    }

    @Test
    @DisplayName("Branching follow-up tree (one consultation leading to two independent follow-ups) aggregates all branches")
    void testBranchingFollowUpTreeCost() {
        LocalDateTime t1 = LocalDateTime.of(2026, 9, 15, 9, 0);
        Appointment root = appointmentDao.save(new Appointment(null, 10L, 1L, t1, "Root Consultation", AppointmentStatus.COMPLETED, null));

        // Two independent follow-ups attached to the same root visit
        Appointment childA = appointmentDao.save(new Appointment(null, 10L, 1L, t1.plusDays(3), "Lab Checkup A", AppointmentStatus.SCHEDULED, root.getId()));
        Appointment childB = appointmentDao.save(new Appointment(null, 10L, 2L, t1.plusDays(5), "Specialist Review B", AppointmentStatus.SCHEDULED, root.getId()));

        // Root (1500) + ChildA (1500) + ChildB (2200) = MUR 5200.0
        double totalCost = appointmentService.calculateTotalChainCost(root.getId());
        assertEquals(5200.0, totalCost);

        CareChainSummary summary = appointmentService.summarizeCareChain(root.getId());
        assertEquals(3, summary.getTotalVisits());
        assertEquals(2, summary.getMaxChainDepth(), "Branching tree with 2 children has depth 2");
    }

    @Test
    @DisplayName("Boundary Condition: A 6-level chain (the maximum allowed depth) executes and succeeds")
    void testMaxAllowedDepthSixSucceeds() {
        LocalDateTime time = LocalDateTime.of(2026, 9, 15, 8, 30);
        Long currentParentId = null;
        Long rootId = null;

        // Build a linear chain of exactly 6 visits (depth 1 to 6)
        for (int i = 1; i <= 6; i++) {
            Appointment appt = appointmentDao.save(new Appointment(null, 10L, 1L, time.plusDays(i), "Step " + i, AppointmentStatus.SCHEDULED, currentParentId));
            if (i == 1) rootId = appt.getId();
            currentParentId = appt.getId();
        }

        // 6 visits x MUR 1500 = MUR 9000.0
        double totalCost = appointmentService.calculateTotalChainCost(rootId);
        assertEquals(9000.0, totalCost);

        CareChainSummary summary = appointmentService.summarizeCareChain(rootId);
        assertEquals(6, summary.getTotalVisits());
        assertEquals(6, summary.getMaxChainDepth(), "Max depth must equal exactly 6");
    }

    @Test
    @DisplayName("Boundary Violation: A 7-level chain exceeding MAX_CARE_CHAIN_DEPTH throws IllegalStateException")
    void testExceedingMaxDepthSevenThrowsException() {
        LocalDateTime time = LocalDateTime.of(2026, 9, 15, 8, 30);
        Long currentParentId = null;
        Long rootId = null;

        // Build a chain of 7 visits (depth 7 exceeds MAX_CARE_CHAIN_DEPTH = 6)
        for (int i = 1; i <= 7; i++) {
            Appointment appt = appointmentDao.save(new Appointment(null, 10L, 1L, time.plusDays(i), "Step " + i, AppointmentStatus.SCHEDULED, currentParentId));
            if (i == 1) rootId = appt.getId();
            currentParentId = appt.getId();
        }

        final Long targetRootId = rootId;
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            appointmentService.calculateTotalChainCost(targetRootId);
        });

        assertTrue(ex.getMessage().contains("exceeds maximum allowed clinical depth of 6"));
    }

    @Test
    @DisplayName("Cycle Guard: A circular reference in follow-up chain throws IllegalStateException")
    void testCircularReferenceThrowsException() {
        LocalDateTime time = LocalDateTime.of(2026, 9, 15, 9, 0);

        // Create visit 1
        Appointment v1 = appointmentDao.save(new Appointment(1001L, 10L, 1L, time, "V1", AppointmentStatus.SCHEDULED, null));

        // Create visit 2 referencing visit 1 as parent
        Appointment v2 = appointmentDao.save(new Appointment(1002L, 10L, 1L, time.plusDays(2), "V2", AppointmentStatus.SCHEDULED, 1001L));

        // Malicious or corrupted database row: Visit 1's parent updated to point to Visit 2 (Creating 1001 -> 1002 -> 1001 cycle!)
        Appointment cycleChild = appointmentDao.save(new Appointment(1003L, 10L, 1L, time.plusDays(4), "V3 (Loop)", AppointmentStatus.SCHEDULED, 1002L));
        // Force cycle: add child to 1002 that points back to 1001
        appointmentDao.save(new Appointment(1004L, 10L, 1L, time.plusDays(6), "V4 (Loop)", AppointmentStatus.SCHEDULED, 1001L));

        // Create a direct cycle: 1001 is parent of 1002, and 1002 is parent of 1001
        InMemoryAppointmentDao cycleDao = new InMemoryAppointmentDao();
        Appointment nodeA = cycleDao.save(new Appointment(2001L, 10L, 1L, time, "Node A", AppointmentStatus.SCHEDULED, null));
        Appointment nodeB = cycleDao.save(new Appointment(2002L, 10L, 1L, time.plusDays(1), "Node B", AppointmentStatus.SCHEDULED, 2001L));
        // Force cycle by adding a child with parent 2002 whose ID is 2001 (or recursing back)
        Appointment nodeC = cycleDao.save(new Appointment(2001L, 10L, 1L, time.plusDays(2), "Node A Loop", AppointmentStatus.SCHEDULED, 2002L));

        AppointmentService cycleService = new AppointmentService(cycleDao, doctorDao);
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            cycleService.calculateTotalChainCost(2001L);
        });

        assertTrue(ex.getMessage().contains("Circular reference detected"));
    }
}