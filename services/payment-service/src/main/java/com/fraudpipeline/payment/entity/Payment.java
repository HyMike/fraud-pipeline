package com.fraudpipeline.payment.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class Payment {

    public enum Status {
        INITIATED, FRAUD_SCORED, SETTLED, FLAGGED, APPROVED, BLOCKED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_id", nullable = false)
    private Merchant merchant;

    @Column(nullable = false)
    private BigDecimal amount;

    @Builder.Default
    @Column(nullable = false)
    private String currency = "USD";

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.INITIATED;

    @Column(name = "idempotency_key", nullable = false)
    private String idempotencyKey;

    @Column(name = "fraud_score")
    private BigDecimal fraudScore;

    @Builder.Default
    @Column(name = "created_at")
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Builder.Default
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt = OffsetDateTime.now();
}
