package com.clinic.service;

import com.clinic.dao.AppointmentDao;
import com.clinic.dao.DoctorDao;
import com.clinic.model.Appointment;
import com.clinic.model.AppointmentStatus;
import com.clinic.model.Doctor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

// Service layer component managing clinic appointment domain operations
// Implements Bounded Recursion for follow-up care chain cost calculations and tree reconstruction
public class AppointmentService {

    // Maximum allowable clinical follow-up care chain depth per project specs
    public static final int MAX_CARE_CHAIN_DEPTH = 6;

    // Default consultation rate fallback in MUR if doctor record is unassigned
    public static final double DEFAULT_CONSULTATION_RATE_MUR = 1500.0;

    // Data access object dependencies
    private final AppointmentDao appointmentDao;
    private final DoctorDao doctorDao;

    // Constructor injecting dependencies for persistencies
    public AppointmentService(AppointmentDao appointmentDao, DoctorDao doctorDao) {
        this.appointmentDao = Objects.requireNonNull(appointmentDao, "AppointmentDao cannot be null.");
        this.doctorDao = Objects.requireNonNull(doctorDao, "DoctorDao cannot be null.");
    }
    // Retrieves the consultation fee in Mauritian Rupees (MUR) based on the attending doctor's hourly rate
    private double getAppointmentCostMur(Appointment appointment) {
        if (appointment == null || appointment.getDoctorId() == null) {
            return DEFAULT_CONSULTATION_RATE_MUR;
        }

        // Query doctor from database to retrieve their official consultation rate in MUR
        return doctorDao.findById(appointment.getDoctorId())
                .map(Doctor::getHourlyRate)
                .orElse(DEFAULT_CONSULTATION_RATE_MUR);
    }

    // Recursively calculates the total treatment cost across a connected follow-up care chain
    // Enforces bounded recursion depth (<= 6) and correct appointment follow-up cycle
    public double calculateTotalChainCost(Long appointmentId, int currentDepth, Set<Long> visited) {
        if (appointmentId == null) {
            return 0.0;
        }

        // Bounded Depth Guard Constraint
        if (currentDepth > MAX_CARE_CHAIN_DEPTH) {
            throw new IllegalStateException("Follow-up appointments exceeds maximum allowed clinical depth of " +
                    MAX_CARE_CHAIN_DEPTH + " visits.");
        }

        // Protects against appointments pointing back to each other as follow-ups
        if (!visited.add(appointmentId)) {
            throw new IllegalStateException("Circular reference detected in follow-up appointment at appointment #" + appointmentId);
        }

        // Retrieve the current appointment record from SQLite
        Optional<Appointment> apptOpt = appointmentDao.findById(appointmentId);
        if (apptOpt.isEmpty()) {
            return 0.0;
        }

        Appointment currentAppt = apptOpt.get();

        // Cancelled appointments are omitted from financial treatment totals
        double currentCost = (currentAppt.getStatus() != AppointmentStatus.CANCELLED)
                ? getAppointmentCostMur(currentAppt)
                : 0.0;

        // Query direct child follow-up appointments attached to this parent visit
        List<Appointment> childFollowUps = appointmentDao.findByParentAppointmentId(appointmentId);

        // Base Case: Leaf appointment with no child follow-ups returns its own cost
        if (childFollowUps.isEmpty()) {
            return currentCost;
        }

        // Recursive Step: Add this appointment's cost to the recursive sum of all child follow-up visits
        double totalChainCost = currentCost;
        for (Appointment child : childFollowUps) {
            totalChainCost += calculateTotalChainCost(child.getId(), currentDepth + 1, visited);
        }

        return totalChainCost;
    }
    // Recursively gathers all connected follow-up appointments in a chronological order
    public void getFollowUpChainTree(Long appointmentId, int currentDepth,
                                     List<Appointment> accumulatedChain, Set<Long> visited) {
        if (appointmentId == null) {
            return;
        }

        // Bounded depth enforcement
        if (currentDepth > MAX_CARE_CHAIN_DEPTH) {
            throw new IllegalStateException("Follow-up care chain exceeds maximum allowed clinical depth of " +
                    MAX_CARE_CHAIN_DEPTH + " visits.");
        }

        // Cycle detection enforcement
        if (!visited.add(appointmentId)) {
            throw new IllegalStateException("Circular reference detected in follow-up chain at appointment #" + appointmentId);
        }

        Optional<Appointment> apptOpt = appointmentDao.findById(appointmentId);
        if (apptOpt.isEmpty()) {
            return;
        }

        Appointment currentAppt = apptOpt.get();
        accumulatedChain.add(currentAppt);

        // Query direct child follow-ups
        List<Appointment> directChildren = appointmentDao.findByParentAppointmentId(appointmentId);

        // Base case: Leaf node with no further follow-up visits terminates recursion
        if (directChildren.isEmpty()) {
            return;
        }

        // Recursive step: Recurse into each child follow-up branch
        for (Appointment child : directChildren) {
            getFollowUpChainTree(child.getId(), currentDepth + 1, accumulatedChain, visited);
        }
    }
    // Recursively computes the maximum tree depth reached by any branch in the follow-up care chain
    public int calculateMaxDepth(Long appointmentId, int currentDepth, Set<Long> visited) {
        if (appointmentId == null) return 0;

        if (currentDepth > MAX_CARE_CHAIN_DEPTH) {
            throw new IllegalStateException("Care chain exceeds maximum depth of " + MAX_CARE_CHAIN_DEPTH);
        }

        if (!visited.add(appointmentId)) {
            throw new IllegalStateException("Circular reference detected at appointment #" + appointmentId);
        }

        List<Appointment> children = appointmentDao.findByParentAppointmentId(appointmentId);

        // Base Case: If this is a leaf node, this branch's depth is currentDepth
        if (children.isEmpty()) {
            return currentDepth;
        }

        // Recursive Step: Compute the maximum depth among all child follow-up branches
        int maxDepth = currentDepth;
        for (Appointment child : children) {
            int branchDepth = calculateMaxDepth(child.getId(), currentDepth + 1, visited);
            if (branchDepth > maxDepth) {
                maxDepth = branchDepth;
            }
        }

        return maxDepth;
    }

    // Recursively counts the total number of visits across the treatment plan
    public int countTotalVisits(Long appointmentId, int currentDepth, Set<Long> visited) {
        if (appointmentId == null) return 0;

        if (currentDepth > MAX_CARE_CHAIN_DEPTH) {
            throw new IllegalStateException("Care chain exceeds maximum depth of " + MAX_CARE_CHAIN_DEPTH);
        }

        if (!visited.add(appointmentId)) {
            throw new IllegalStateException("Circular reference detected at appointment #" + appointmentId);
        }

        List<Appointment> children = appointmentDao.findByParentAppointmentId(appointmentId);

        // Base case: leaf node represents exactly 1 visit
        if (children.isEmpty()) {
            return 1;
        }

        // Recursive step: count this visit plus recursive counts of all child follow-ups
        int count = 1;
        for (Appointment child : children) {
            count += countTotalVisits(child.getId(), currentDepth + 1, visited);
        }

        return count;
    }

    // Public convenience overload calculating total chain cost starting from depth 1
    public double calculateTotalChainCost(Long rootAppointmentId) {
        return calculateTotalChainCost(rootAppointmentId, 1, new HashSet<>());
    }

    // Public convenience overload retrieving the full care chain list
    public List<Appointment> getFollowUpChainTree(Long rootAppointmentId) {
        List<Appointment> chain = new ArrayList<>();
        getFollowUpChainTree(rootAppointmentId, 1, chain, new HashSet<>());
        return chain;
    }

    // High-level service method generating an aggregated CareChainSummary for UI and billing
    public CareChainSummary summarizeCareChain(Long rootAppointmentId) {
        if (rootAppointmentId == null) {
            throw new IllegalArgumentException("Root appointment ID cannot be null.");
        }

        // 1. Gather all connected appointment entities
        List<Appointment> chain = getFollowUpChainTree(rootAppointmentId);

        // 2. Extract ordered sequence of appointment IDs
        List<Long> visitIds = chain.stream().map(Appointment::getId).toList();

        // 3. Calculate total treatment cost in MUR
        double totalCostMur = calculateTotalChainCost(rootAppointmentId, 1, new HashSet<>());

        // 4. Calculate maximum clinical hierarchy depth
        int maxDepth = calculateMaxDepth(rootAppointmentId, 1, new HashSet<>());

        // 5. Total visits count
        int totalVisits = chain.size();

        return new CareChainSummary(rootAppointmentId, totalVisits, maxDepth, totalCostMur, visitIds);
    }
}