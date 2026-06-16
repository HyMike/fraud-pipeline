package com.fraudpipeline.payment.repository;

import com.fraudpipeline.payment.entity.JournalEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface JournalEntryRepository extends JpaRepository<JournalEntry, UUID> {
    List<JournalEntry> findByPaymentId(UUID paymentId);
}
