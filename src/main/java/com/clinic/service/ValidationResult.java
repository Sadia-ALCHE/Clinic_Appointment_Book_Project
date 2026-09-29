package com.clinic.service;

import java.util.Objects;

// Permanent value object that represents the outcome of the clinic's business validation rules
// Encapsulates success message modals and rejection messages without throwing exceptions
public final class ValidationResult {

    // Flag indicating whether validation passed
    private final boolean valid;

    // Message explaining rejection reason (null if validation succeeded)
    private final String errorMessage;

    // Private constructor enforcing creation with static factory methods
    private ValidationResult(boolean valid, String errorMessage) {
        this.valid = valid;
        this.errorMessage = errorMessage;
    }

    // Static method for successful validation
    public static ValidationResult valid() {
        return new ValidationResult(true, null);
    }

    // Static method for failed validation with an explanatory message
    public static ValidationResult invalid(String errorMessage) {
        if (errorMessage == null || errorMessage.trim().isEmpty()) {
            throw new IllegalArgumentException("Validation error message cannot be null or empty.");
        }
        return new ValidationResult(false, errorMessage.trim());
    }

    // Returns true if validation passed cleanly with zero violations
    public boolean isValid() {
        return valid;
    }

    // Returns true if validation failed
    public boolean hasError() {
        return !valid;
    }

    // Returns the rejection reason, or null if valid
    public String getErrorMessage() {
        return errorMessage;
    }

    // Returns formatted error message string for logging and debugging
    @Override
    public String toString() {
        if (valid) {
            return "ValidationResult[VALID]";
        }
        return "ValidationResult[INVALID: " + errorMessage + "]";
    }

    // Value object equality based on valid status and error message
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ValidationResult that)) return false;
        return valid == that.valid && Objects.equals(errorMessage, that.errorMessage);
    }

    // Hash code generation consistent with equals
    @Override
    public int hashCode() {
        return Objects.hash(valid, errorMessage);
    }
}