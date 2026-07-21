package com.example.kyc_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.kyc_service.model.KycSubmission;

public interface KycSubmissionRepository extends JpaRepository<KycSubmission,Long>{
    
}
