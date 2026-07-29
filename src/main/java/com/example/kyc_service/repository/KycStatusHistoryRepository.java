package com.example.kyc_service.repository;

import com.example.kyc_service.model.KycStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface KycStatusHistoryRepository extends JpaRepository<KycStatusHistory, Long> {

    List<KycStatusHistory> findBySubmissionIdOrderByChangedAtAsc(UUID submissionId);
}