package com.example.kyc_service.controller;

import com.example.kyc_service.dto.*;
import com.example.kyc_service.enums.DocumentType;
import com.example.kyc_service.enums.SubmissionStatus;
import com.example.kyc_service.service.KycSubmissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/kyc/analyst")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ROLE_KYC_ANALYST', 'ROLE_ADMIN', 'ROLE_SUPERADMIN')")
public class KycAnalystController {

    private final KycSubmissionService service;

    @GetMapping("/submissions")
    public ResponseEntity<Page<AnalystSubmissionResponse>> list(
            @RequestParam(required = false) SubmissionStatus status,
            @RequestParam(required = false) DocumentType documentType,
            @RequestParam(required = false) String username,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        return ResponseEntity.ok(
                service.listForAnalyst(status, documentType, username, PageRequest.of(page, size, sort)));
    }

    @GetMapping("/submissions/{id}")
    public ResponseEntity<AnalystSubmissionResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(service.getForAnalyst(id));
    }

    @GetMapping("/submissions/{id}/document-url")
    public ResponseEntity<Map<String, String>> getDocumentUrl(@PathVariable UUID id) {
        return ResponseEntity.ok(Map.of("url", service.generateDocumentUrl(id)));
    }

    @PostMapping("/submissions/{id}/decision")
    public ResponseEntity<AnalystSubmissionResponse> decide(
            @PathVariable UUID id,
            @Valid @RequestBody AnalystDecisionRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        Long analystId = extractUserId(jwt);
        String analystUsername = jwt.getSubject();
        return ResponseEntity.ok(service.processDecision(id, request, analystId, analystUsername));
    }

    @GetMapping("/submissions/{id}/history")
    public ResponseEntity<List<StatusHistoryResponse>> history(@PathVariable UUID id) {
        return ResponseEntity.ok(service.getHistory(id));
    }

    @GetMapping("/metrics")
    public ResponseEntity<KycMetricsResponse> metrics() {
        return ResponseEntity.ok(service.getMetrics());
    }

    // ── Expected data (pre-registration for auto-decision) ──────────────────────

    @PostMapping("/expected-data")
    public ResponseEntity<ExpectedDataResponse> registerExpectedData(
            @Valid @RequestBody RegisterExpectedDataRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        Long analystId = extractUserId(jwt);
        String analystUsername = jwt.getSubject();
        return ResponseEntity.status(201)
                .body(service.registerExpectedData(request, analystId, analystUsername));
    }

    @GetMapping("/expected-data")
    public ResponseEntity<List<ExpectedDataResponse>> listExpectedData(
            @RequestParam Long userId) {
        return ResponseEntity.ok(service.listExpectedData(userId));
    }

    // ── Private ───────────────────────────────────────────────────────────────

    /**
     * Robust userId extraction from the JWT claim.
     * Same fix already applied to KycClientController#submit(): the raw claim
     * can come back as Integer, Long or String depending on how the auth-service
     * serialized it, so a bare `jwt.getClaim("userId")` cast risks a
     * ClassCastException at runtime. This mirrors that logic so both
     * controllers behave consistently.
     */
    private Long extractUserId(Jwt jwt) {
        Object raw = jwt.getClaim("userId");
        if (raw == null) raw = jwt.getClaim("id");
        if (raw == null) raw = jwt.getClaim("user_id");

        if (raw instanceof Number number) {
            return number.longValue();
        }
        if (raw instanceof String str) {
            return Long.parseLong(str);
        }
        throw new IllegalArgumentException("O claim com o ID do usuário não foi encontrado no JWT.");
    }
}