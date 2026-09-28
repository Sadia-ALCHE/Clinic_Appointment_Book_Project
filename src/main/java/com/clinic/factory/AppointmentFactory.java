package com.clinic.factory;

import com.clinic.model.Appointment;
import com.clinic.model.AppointmentStatus;
import com.clinic.model.AppointmentType;

import java.time.LocalDateTime;
import java.util.Objects;

// Creates appointments using predefined appointment types and rules.
public final class AppointmentFactory {

    // Default reasons used when creating common appointment types.
    public static final String DEFAULT_STANDARD_REASON = "Standard Outpatient Clinical Consultation";
    public static final String DEFAULT_FOLLOW_UP_REASON = "Post-Treatment Clinical Follow-Up Review";
    public static final String DEFAULT_EMERGENCY_REASON = "Urgent Acute Medical Care / Emergency Triage";

    // Private constructor prevents this utility class from being instantiated.
    private AppointmentFactory() {
        // AppointmentFactory only provides static methods.
    }
}