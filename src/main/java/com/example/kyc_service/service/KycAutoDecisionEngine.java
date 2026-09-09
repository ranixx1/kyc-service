package com.example.kyc_service.service;

import com.example.kyc_service.enums.DocumentType;
import com.example.kyc_service.model.KycExpectedData;
import com.example.kyc_service.model.KycSubmission;
import com.example.kyc_service.repository.KycExpectedDataRepository;
import com.example.kyc_service.service.analysis.DocumentAnalysis;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Decides whether a submission can be APPROVED or REJECTED automatically,
 * without a human analyst, by comparing OCR-extracted fields against data
 * an analyst pre-registered for that user (KycExpectedData).
 *
 * Hard rules — no auto-decision happens unless ALL of these hold:
 *   1. The document type is an identity document (ID_CARD, DRIVER_LICENSE, PASSPORT).
 *      Bank statements, pay slips, and bills don't carry a clear "expected holder name
 *      + document number" pair to check identity against.
 *   2. OCR + validation both passed (analysis.isPassed()).
 *   3. Fraud detection came back clean. ANY fraud indicator forces manual review —
 *      this engine will never auto-approve a flagged document, and treats a fraud
 *      signal as "cannot decide" rather than "reject", since a false positive fraud
 *      rule shouldn't auto-reject a legitimate user.
 *   4. There's an unconsumed KycExpectedData record for (userId, documentType).
 *   5. Both holderName and documentNumber were actually extracted.
 *
 * If all of that holds, extracted vs. expected values are compared after
 * normalization (case/accents/punctuation-insensitive). Exact match on both
 * fields -> APPROVE. Anything else -> REJECT (INCONSISTENT_DATA), since the
 * whole point of pre-registering data is to catch someone submitting a
 * document for a different identity than expected.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KycAutoDecisionEngine {

    private static final Set<DocumentType> AUTO_DECIDABLE_TYPES = Set.of(
            DocumentType.IDENTITY_CARD, DocumentType.DRIVER_LICENSE, DocumentType.PASSPORT);

    private final KycExpectedDataRepository expectedDataRepository;

    public enum Outcome {
        UNSUPPORTED_TYPE,
        INSUFFICIENT_DATA,
        FRAUD_SUSPECTED,
        NO_EXPECTED_DATA,
        MATCH,
        MISMATCH
    }

    public record Decision(Outcome outcome, String reason, KycExpectedData expectedData) {
        public boolean isApprove() {
            return outcome == Outcome.MATCH;
        }

        public boolean isReject() {
            return outcome == Outcome.MISMATCH;
        }
    }

    public Decision evaluate(KycSubmission submission, DocumentAnalysis analysis) {
        DocumentType type = submission.getDocumentType();

        if (!AUTO_DECIDABLE_TYPES.contains(type)) {
            return new Decision(Outcome.UNSUPPORTED_TYPE,
                    "Auto-decision only supported for identity documents (ID_CARD, DRIVER_LICENSE, PASSPORT).",
                    null);
        }

        if (analysis.getFraudResult() != null && analysis.getFraudResult().suspicious()) {
            return new Decision(Outcome.FRAUD_SUSPECTED,
                    "Fraud indicators present, deferring to manual review: " + analysis.getFraudResult().summary(),
                    null);
        }

        if (!analysis.isPassed()) {
            return new Decision(Outcome.INSUFFICIENT_DATA,
                    "OCR/validation did not pass; cannot auto-decide.", null);
        }

        Optional<KycExpectedData> expectedOpt = expectedDataRepository
                .findFirstByUserIdAndDocumentTypeAndConsumedFalseOrderByCreatedAtAsc(
                        submission.getUserId(), type);

        if (expectedOpt.isEmpty()) {
            return new Decision(Outcome.NO_EXPECTED_DATA,
                    "No pre-registered expected data for this user/documentType.", null);
        }

        KycExpectedData expected = expectedOpt.get();
        Map<String, String> fields = analysis.getExtractedFields();

        String extractedName = fields == null ? null : fields.get("holderName");
        String extractedNumber = fields == null ? null : firstNonBlank(
                fields.get("documentNumber"), fields.get("licenseNumber"), fields.get("passportNumber"));

        if (isBlank(extractedName) || isBlank(extractedNumber)) {
            return new Decision(Outcome.INSUFFICIENT_DATA,
                    "Holder name or document number was not extracted from the document.", expected);
        }

        boolean nameMatches = normalizeName(extractedName).equals(normalizeName(expected.getExpectedHolderName()));
        boolean numberMatches = normalizeNumber(extractedNumber).equals(normalizeNumber(expected.getExpectedDocumentNumber()));

        if (nameMatches && numberMatches) {
            log.info("Auto-decision MATCH. submissionId={}, userId={}, documentType={}",
                    submission.getId(), submission.getUserId(), type);
            return new Decision(Outcome.MATCH, "Extracted data matches pre-registered expected data.", expected);
        }

        StringBuilder reason = new StringBuilder("Extracted data does not match pre-registered expected data.");
        if (!nameMatches) reason.append(" Holder name mismatch.");
        if (!numberMatches) reason.append(" Document number mismatch.");

        log.warn("Auto-decision MISMATCH. submissionId={}, userId={}, documentType={}, reason={}",
                submission.getId(), submission.getUserId(), type, reason);
        return new Decision(Outcome.MISMATCH, reason.toString(), expected);
    }

    private String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) return v;
        }
        return null;
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private String normalizeName(String name) {
        String noAccents = Normalizer.normalize(name, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return noAccents.trim().toUpperCase().replaceAll("\\s+", " ");
    }

    private String normalizeNumber(String number) {
        return number.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
    }
}