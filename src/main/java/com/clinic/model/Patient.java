package com.clinic.model;
import java.time.LocalDate;
import java.util.Objects;

//Patient class initialized with private access modifier to encapsulate the internal state
public class Patient {
    private final Long id;
    private String fullName;
    private String phoneNumber;
    private LocalDate birthDate;


    // Parameterized constructors to enforce invariants before allocating values
    public Patient(Long id, String fullName, String phoneNumber, LocalDate dateOfBirth) {
        this.id = id;
        this.fullName = validateName(fullName);
        this.phoneNumber = validatePhone(phoneNumber);
        this.birthDate = Objects.requireNonNull(dateOfBirth, "Date of birth cannot be null.");


    }
    //Using DRY - Don't Repeat Yourself Principle to create name and phone number helper validator methods that belong to the class so same code is not repeated
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

    // Public getters to provide read-only access for UI and billing
    public Long getId() { return id; }
    public String getFullName() { return fullName; }
    public String getPhoneNumber() { return phoneNumber; }
    public LocalDate getDateOfBirth() { return birthDate; }

    // Public setters for state mutation but with invariant checks enabled by the validation methods
    public void setFullName(String fullName) {
        this.fullName = validateName(fullName);
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = validatePhone(phoneNumber);
    }
}



