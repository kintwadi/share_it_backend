package com.vicinity24.api.insurance.exception;

/**
 * Raised when an insurance type is unknown or invalid.
 */
public class InvalidInsuranceTypeException extends RuntimeException {
    public InvalidInsuranceTypeException(String message) {
        super(message);
    }
}

