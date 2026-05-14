package com.nearshare.api.insurance.exception;

/**
 * Raised when a requested resource (e.g., quote) cannot be found.
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}

