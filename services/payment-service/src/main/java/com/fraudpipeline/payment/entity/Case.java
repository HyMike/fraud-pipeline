package com.fraudpipeline.payment.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "cases")
@Getter
@Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class Case {

    public enum Status { PENDING, APPROVED, BLOCKED }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @Column(name = "fraud_score", nullable = false)
    private BigDecimal fraudScore;

    @Column(name = "shap_values", columnDefinition = "jsonb", nullable = false)
    private String shapValues;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.PENDING;

    @Column(name = "analyst_id")
    private String analystId;

    @Column(name = "notes")
    private String notes;

    @Column(name = "decision_at")
    private OffsetDateTime decisionAt;

    @Builder.Default
    @Column(name = "created_at")
    private OffsetDateTime createdAt = OffsetDateTime.now();
}
