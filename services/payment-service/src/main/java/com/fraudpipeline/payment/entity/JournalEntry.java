package com.fraudpipeline.payment.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "journal_entries")
@Getter
@Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class JournalEntry {

    public enum Direction { DEBIT, CREDIT }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Direction direction;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private String currency;

    @Builder.Default
    @Column(name = "created_at")
    private OffsetDateTime createdAt = OffsetDateTime.now();
}
