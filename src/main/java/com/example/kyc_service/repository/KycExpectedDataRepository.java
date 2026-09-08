package com.example.kyc_service.repository;

import com.example.kyc_service.enums.DocumentType;
import com.example.kyc_service.model.KycExpectedData;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface KycExpectedDataRepository extends JpaRepository<KycExpectedData, UUID> {

    Optional<KycExpectedData> findFirstByUserIdAndDocumentTypeAndConsumedFalseOrderByCreatedAtAsc(
            Long userId, DocumentType documentType);

    List<KycExpectedData> findByUserIdOrderByCreatedAtDesc(Long userId);

    boolean existsByUserIdAndDocumentTypeAndConsumedFalse(Long userId, DocumentType documentType);
}