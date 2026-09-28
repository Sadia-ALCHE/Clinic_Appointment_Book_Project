package com.clinic.model;

// Types of appointments available in the clinic
public enum AppointmentType {

    STANDARD_CONSULTATION("Standard Consultation"),
    FOLLOW_UP("Follow-Up Visit"),
    EMERGENCY("Emergency Checkup");

    private final String displayName;

    AppointmentType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    // Makes the display name appear in JavaFX controls
    @Override
    public String toString() {
        return displayName;
    }
}