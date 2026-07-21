package com.example.kyc_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.kyc_service.model.KycStatusHistory;

public interface KycStatusHistoryRepository extends JpaRepository<KycStatusHistory,Long>{
    
}
