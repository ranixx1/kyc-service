package com.example.kyc_service.controller;

import com.example.kyc_service.dto.SubmissionResponse;
import com.example.kyc_service.enums.DocumentType;
import com.example.kyc_service.service.KycSubmissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/kyc/submissions")
@RequiredArgsConstructor
public class KycClientController {

    private final KycSubmissionService service;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SubmissionResponse> submit(
            @RequestParam("file") MultipartFile file,
            @RequestParam("documentType") DocumentType documentType,
            @AuthenticationPrincipal Jwt jwt) {

        Long userId = extractUserId(jwt);
        String username = jwt.getSubject();
        return ResponseEntity.status(201).body(service.submit(file, documentType, userId, username));
    }

    @GetMapping
    public ResponseEntity<List<SubmissionResponse>> listMine(@AuthenticationPrincipal Jwt jwt) {
        Long userId = extractUserId(jwt);
        return ResponseEntity.ok(service.listMySubmissions(userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SubmissionResponse> getMine(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        Long userId = extractUserId(jwt);
        return ResponseEntity.ok(service.getMySubmission(id, userId));
    }

    // ── Private ───────────────────────────────────────────────────────────────

    /**
     * Robust userId extraction from the JWT claim.
     * The raw claim can come back as Integer, Long or String depending on how
     * the auth-service serialized it — a bare `jwt.getClaim("userId")` cast to
     * Long risks a ClassCastException at runtime. Applied consistently across
     * every endpoint here (previously only submit() had this handling).
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