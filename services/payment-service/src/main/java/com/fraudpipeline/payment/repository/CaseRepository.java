package com.fraudpipeline.payment.repository;

import com.fraudpipeline.payment.entity.Case;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.UUID;

public interface CaseRepository extends JpaRepository<Case, UUID> {
    Page<Case> findByStatus(Case.Status status, Pageable pageable);
}
