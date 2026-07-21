package com.example.kyc_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.kyc_service.model.KycStatusHistory;
import java.util.List;
import java.util.UUID;

public interface KycStatusHistoryRepository extends JpaRepository<KycStatusHistory, Long> {
    List<KycStatusHistory> findBySubmissionIdOrderByChangedAtAsc(UUID submissionId);
}