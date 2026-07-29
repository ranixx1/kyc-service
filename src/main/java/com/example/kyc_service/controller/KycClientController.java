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

        // ADICIONE ESTA LINHA PARA VER NO TERMINAL:
        System.out.println(">>> CLAIMS PRESENTES NO JWT: " + jwt.getClaims());

        Object rawUserId = jwt.getClaim("userId");
        if (rawUserId == null)
            rawUserId = jwt.getClaim("id");
        if (rawUserId == null)
            rawUserId = jwt.getClaim("user_id");
        Long userId = null;
        if (rawUserId instanceof Number number) {
            userId = number.longValue();
        } else if (rawUserId instanceof String str) {
            userId = Long.parseLong(str);
        }

        // Validação preventiva para não estourar erro 500 no MySQL
        if (userId == null) {
            throw new IllegalArgumentException("O claim com o ID do usuário não foi encontrado no JWT.");
        }

        String username = jwt.getSubject();
        return ResponseEntity.status(201).body(service.submit(file, documentType, userId, username));
    }

    @GetMapping
    public ResponseEntity<List<SubmissionResponse>> listMine(@AuthenticationPrincipal Jwt jwt) {
        Long userId = jwt.getClaim("userId");
        return ResponseEntity.ok(service.listMySubmissions(userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SubmissionResponse> getMine(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        Long userId = jwt.getClaim("userId");
        return ResponseEntity.ok(service.getMySubmission(id, userId));
    }
}