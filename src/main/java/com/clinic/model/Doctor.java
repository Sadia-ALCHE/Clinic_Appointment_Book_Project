package com.clinic.model;

//Doctor class declared with private fields to seal the data
public class Doctor {
    private final Long id;
    private String fullName;
    private String specialty;
    private double hourlyRate;

    //Parameterized constructors to enforce invariants at the moment of creation
    //validateName method used as declared in Patient.java file using the DRY principle
    public Doctor(Long id, String name, String specialty, double hourlyRate) {
        this.id = id;
        this.fullName = Patient.validateName(name);
        this.specialty = (specialty == null || specialty.trim().isEmpty()) ? "General Practice" : specialty.trim();
        if (hourlyRate < 500.0 || hourlyRate > 10000.0) {
            throw new IllegalArgumentException("Hourly rate must be between MUR 500.00 and MUR 10,000.00.");
        }
        this.hourlyRate = hourlyRate;
    }
    //public Getters providing read-only access
    public Long getId() { return id; }
    public String getName() { return fullName; }
    public String getSpecialty() { return specialty; }
    public double getHourlyRate() { return hourlyRate; }
}
