package com.example.kyc_service.exception;

import java.util.UUID;

public class SubmissionNotFoundException extends RuntimeException {
    public SubmissionNotFoundException(UUID id) {
        super("Submission not found: " + id);
    }
}