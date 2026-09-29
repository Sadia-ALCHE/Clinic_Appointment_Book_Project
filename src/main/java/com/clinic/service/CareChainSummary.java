package com.clinic.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

// Creating a value object that represents the metrics of a recursive care chain
// Encapsulates root ID, visit count, maximum recursion depth, total cost in MUR, and node IDs
public final class CareChainSummary {

    private final Long rootAppointmentId;
    private final int totalVisits;
    private final int maxChainDepth;
    private final double totalCostMur;
    private final List<Long> visitIds;

    public CareChainSummary(Long rootAppointmentId, int totalVisits, int maxChainDepth,
                            double totalCostMur, List<Long> visitIds) {
        this.rootAppointmentId = Objects.requireNonNull(rootAppointmentId, "Root appointment ID cannot be null.");
        this.totalVisits = totalVisits;
        this.maxChainDepth = maxChainDepth;
        this.totalCostMur = totalCostMur;
        this.visitIds = (visitIds != null) ? List.copyOf(visitIds) : Collections.emptyList();
    }

    public Long getRootAppointmentId() {
        return rootAppointmentId;
    }

    public int getTotalVisits() {
        return totalVisits;
    }

    public int getMaxChainDepth() {
        return maxChainDepth;
    }

    public double getTotalCostMur() {
        return totalCostMur;
    }

    public List<Long> getVisitIds() {
        return visitIds;
    }

    @Override
    public String toString() {
        return "CareChainSummary{" +
                "rootId=" + rootAppointmentId +
                ", totalVisits=" + totalVisits +
                ", maxDepth=" + maxChainDepth +
                ", totalCostMur=MUR " + totalCostMur +
                ", visitIds=" + visitIds +
                '}';
    }
}