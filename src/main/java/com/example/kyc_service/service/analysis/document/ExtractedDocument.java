package com.example.kyc_service.service.analysis.document;


import com.example.kyc_service.enums.DocumentType;

import java.util.Map;

/**
 * Base contract for all typed extracted documents.
 *
 * Each implementation holds only the fields relevant to its document type.
 * This avoids the generic Map<String, String> anti-pattern where callers
 * have to know magic string keys to get values.
 *
 * Implementations:
 * - IdentityDocument  (ID_CARD)
 * - DriverLicenseDocument (DRIVER_LICENSE)
 * - PassportDocument  (PASSPORT)
 * - BankStatementDocument (BANK_STATEMENT)
 * - PaySlipDocument   (PAY_SLIP)
 * - UtilityBillDocument (UTILITY_BILL)
 * - PhoneBillDocument (PHONE_BILL)
 */
public interface ExtractedDocument {

    DocumentType getType();

    /**
     * Returns all extracted fields as a flat map.
     * Used for serialization, audit, and display in the analyst panel.
     * Keys are human-readable field names (e.g. "holderName", "documentNumber").
     */
    Map<String, String> toFieldMap();
}