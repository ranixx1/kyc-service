package com.example.kyc_service.repository;

import com.example.kyc_service.enums.DocumentType;
import com.example.kyc_service.enums.SubmissionStatus;
import com.example.kyc_service.model.KycSubmission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface KycSubmissionRepository extends JpaRepository<KycSubmission, UUID> {

    List<KycSubmission> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<KycSubmission> findByIdAndUserId(UUID id, Long userId);

    boolean existsByUserIdAndDocumentTypeAndStatusIn(
            Long userId, DocumentType documentType, List<SubmissionStatus> statuses);

    @Query("SELECT s FROM KycSubmission s WHERE " +
           "(:status IS NULL OR s.status = :status) AND " +
           "(:documentType IS NULL OR s.documentType = :documentType) AND " +
           "(:username IS NULL OR LOWER(s.username) LIKE LOWER(CONCAT('%', :username, '%')))")
    Page<KycSubmission> findWithFilters(
            @Param("status") SubmissionStatus status,
            @Param("documentType") DocumentType documentType,
            @Param("username") String username,
            Pageable pageable);

    @Query("SELECT s.documentType, COUNT(s) FROM KycSubmission s GROUP BY s.documentType")
    List<Object[]> countByDocumentType();

    @Query("SELECT s.status, COUNT(s) FROM KycSubmission s GROUP BY s.status")
    List<Object[]> countGroupedByStatus();
}