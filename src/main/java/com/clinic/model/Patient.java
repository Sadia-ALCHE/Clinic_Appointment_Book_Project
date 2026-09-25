package com.clinic.model;

import java.time.LocalDate;
import java.util.Objects;

// Patient class initialized with private fields to encapsulate patient records
public class Patient {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private LocalDate birthDate;
    private String bloodGroup;

    // Full constructor matching the SQLite schema (7 columns)
    public Patient(Long id, String firstName, String lastName, String email,
                   String phone, LocalDate dateOfBirth, String bloodGroup) {
        this.id = id;
        this.firstName = validateName(firstName);
        this.lastName = validateName(lastName);
        this.email = validateEmail(email);
        this.phone = validatePhone(phone);
        this.birthDate = Objects.requireNonNull(dateOfBirth, "Date of birth cannot be null.");
        this.bloodGroup = validateBloodGroup(bloodGroup);
    }

    // Overloaded constructor for brand new patients before database assigns an ID
    public Patient(String firstName, String lastName, String email,
                   String phone, LocalDate dateOfBirth, String bloodGroup) {
        this(null, firstName, lastName, email, phone, dateOfBirth, bloodGroup);
    }

    // Backward-compatible constructor for earlier code using fullName
    public Patient(Long id, String fullName, String phoneNumber, LocalDate dateOfBirth) {
        this(id, splitFirstName(fullName), splitLastName(fullName),
                generatePlaceholderEmail(fullName), phoneNumber, dateOfBirth, "O+");
    }

    // Static helper validators following DRY principle
    public static String validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Patient name cannot be empty.");
        }
        return name.trim();
    }

    public static String validatePhone(String phone) {
        if (phone == null || phone.replaceAll("[^0-9]", "").length() < 8) {
            throw new IllegalArgumentException("Phone number must contain at least 8 digits.");
        }
        return phone.trim();
    }

    public static String validateEmail(String email) {
        if (email == null || !email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            throw new IllegalArgumentException("Invalid email format: " + email);
        }
        return email.trim();
    }

    public static String validateBloodGroup(String bloodGroup) {
        if (bloodGroup == null || bloodGroup.trim().isEmpty()) {
            throw new IllegalArgumentException("Blood group cannot be empty.");
        }
        return bloodGroup.trim().toUpperCase();
    }

    // Helper methods to split full name into first and last name
    private static String splitFirstName(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) return "Unknown";
        return fullName.trim().split("\\s+", 2)[0];
    }

    private static String splitLastName(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) return "Patient";
        String[] parts = fullName.trim().split("\\s+", 2);
        return parts.length > 1 ? parts[1] : "Patient";
    }

    private static String generatePlaceholderEmail(String fullName) {
        String clean = (fullName == null ? "patient" : fullName.toLowerCase().replaceAll("[^a-z0-9]", ""));
        return clean.isEmpty() ? "patient@clinic.mu" : clean + "@clinic.mu";
    }

    // Public getters to provide read-only access
    public Long getId() { return id; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getFullName() { return (firstName + " " + lastName).trim(); }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getPhoneNumber() { return phone; }
    public LocalDate getDateOfBirth() { return birthDate; }
    public String getBloodGroup() { return bloodGroup; }

    // Public setters for updating state with invariant validation
    public void setId(Long id) { this.id = id; }
    public void setFirstName(String firstName) { this.firstName = validateName(firstName); }
    public void setLastName(String lastName) { this.lastName = validateName(lastName); }
    public void setEmail(String email) { this.email = validateEmail(email); }
    public void setPhone(String phone) { this.phone = validatePhone(phone); }
    public void setPhoneNumber(String phone) { this.phone = validatePhone(phone); }
    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.birthDate = Objects.requireNonNull(dateOfBirth, "Date of birth cannot be null.");
    }
    public void setBloodGroup(String bloodGroup) {
        this.bloodGroup = validateBloodGroup(bloodGroup);
    }
}