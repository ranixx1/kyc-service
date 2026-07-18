package com.example.kyc_service.enums;

/**
 * Lifecycle of a KYC submission.
 *
 * Visible to the CLIENT:       NEW, IN_PROGRESS, APPROVED, REJECTED.
 * Visible to the KYC_ANALYST:   all statuses below
 */

public enum SubmissionStatus {

    NEW,

    IN_PROGRESS,

    /** OCR failed or did not detect sufficient fields. Mandatory human review required. */
    MANUAL,

    /** Analyst approved the submission. Verification complete. */
    APPROVED,

    /** Analyst rejected the submission. Reason stored in rejectReason. */
    REJECTED,
}