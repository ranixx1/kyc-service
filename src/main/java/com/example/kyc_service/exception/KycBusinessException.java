package com.example.kyc_service.exception;

public class KycBusinessException extends RuntimeException {
    public KycBusinessException(String message) {
        super(message);
    }
}