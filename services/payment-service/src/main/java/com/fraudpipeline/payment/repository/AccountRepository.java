package com.fraudpipeline.payment.repository;

import com.fraudpipeline.payment.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface AccountRepository extends JpaRepository<Account, UUID> {
    Optional<Account> findByName(String name);
}
