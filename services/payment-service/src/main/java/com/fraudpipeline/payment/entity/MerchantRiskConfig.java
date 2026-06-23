package com.fraudpipeline.payment.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "merchant_risk_config")
@Getter
@Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class MerchantRiskConfig {

    @Id
    @Column(name = "merchant_id")
    private UUID merchantId;

    @JsonIgnore
    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "merchant_id")
    private Merchant merchant;

    @Builder.Default
    @Column(name = "auto_approve_below")
    private BigDecimal autoApproveBelow = new BigDecimal("0.30");

    @Builder.Default
    @Column(name = "review_above")
    private BigDecimal reviewAbove = new BigDecimal("0.50");

    @Builder.Default
    @Column(name = "auto_block_above")
    private BigDecimal autoBlockAbove = new BigDecimal("0.90");

    @Builder.Default
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt = OffsetDateTime.now();
}
