package com.example.kyc_service.exception;

public class SubmissionNotFoundException extends RuntimeException{
    public SubmissionNotFoundException(String message) {
        super(message);
    }
}
