package com.clinic.model;

// Doctor class declared with private fields to seal physician data
public class Doctor {
    private Long id;
    private String firstName;
    private String lastName;
    private String specialty;
    private double hourlyRate;
    private String email;
    private String phone;

    // Main 7-parameter constructor matching the SQLite doctors table
    public Doctor(Long id, String firstName, String lastName, String specialty,
                  double hourlyRate, String email, String phone) {
        this.id = id;
        this.firstName = Patient.validateName(firstName);
        this.lastName = Patient.validateName(lastName);
        this.specialty = (specialty == null || specialty.trim().isEmpty()) ? "General Practice" : specialty.trim();
        if (hourlyRate < 500.0 || hourlyRate > 10000.0) {
            throw new IllegalArgumentException("Hourly rate must be between MUR 500.00 and MUR 10,000.00.");
        }
        this.hourlyRate = hourlyRate;
        this.email = Patient.validateEmail(email);
        this.phone = Patient.validatePhone(phone);
    }

    // Overloaded constructor for new doctors before database allocates an ID
    public Doctor(String firstName, String lastName, String specialty,
                  double hourlyRate, String email, String phone) {
        this(null, firstName, lastName, specialty, hourlyRate, email, phone);
    }

    // Backward-compatible constructor for earlier 4-parameter calls
    public Doctor(Long id, String name, String specialty, double hourlyRate) {
        this(id, splitFirstName(name), splitLastName(name), specialty, hourlyRate,
                generatePlaceholderEmail(name), "+230 5000 0000");
    }

    // Helper methods to split full name into first and last name
    private static String splitFirstName(String name) {
        if (name == null || name.trim().isEmpty()) return "Unknown";
        return name.trim().split("\\s+", 2)[0];
    }

    private static String splitLastName(String name) {
        if (name == null || name.trim().isEmpty()) return "Doctor";
        String[] parts = name.trim().split("\\s+", 2);
        return parts.length > 1 ? parts[1] : "Doctor";
    }

    private static String generatePlaceholderEmail(String name) {
        String clean = (name == null ? "doctor" : name.toLowerCase().replaceAll("[^a-z0-9]", ""));
        return clean.isEmpty() ? "doctor@clinic.mu" : clean + "@clinic.mu";
    }

    // Public getters providing read-only access
    public Long getId() { return id; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getName() { return (firstName + " " + lastName).trim(); }
    public String getFullName() { return (firstName + " " + lastName).trim(); }
    public String getSpecialty() { return specialty; }
    public double getHourlyRate() { return hourlyRate; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }

    // Public setters for state mutation
    public void setId(Long id) { this.id = id; }
    public void setFirstName(String firstName) { this.firstName = Patient.validateName(firstName); }
    public void setLastName(String lastName) { this.lastName = Patient.validateName(lastName); }
    public void setSpecialty(String specialty) {
        this.specialty = (specialty == null || specialty.trim().isEmpty()) ? "General Practice" : specialty.trim();
    }
    public void setHourlyRate(double hourlyRate) {
        if (hourlyRate < 500.0 || hourlyRate > 10000.0) {
            throw new IllegalArgumentException("Hourly rate must be between MUR 500.00 and MUR 10,000.00.");
        }
        this.hourlyRate = hourlyRate;
    }
    public void setEmail(String email) { this.email = Patient.validateEmail(email); }
    public void setPhone(String phone) { this.phone = Patient.validatePhone(phone); }
}