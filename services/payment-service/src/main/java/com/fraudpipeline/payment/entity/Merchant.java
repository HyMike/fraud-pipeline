package com.fraudpipeline.payment.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "merchants")
@Getter
@Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class Merchant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(name = "api_key", nullable = false, unique = true)
    private String apiKey;

    @Column(name = "callback_url")
    private String callbackUrl;

    @Builder.Default
    @Column(name = "created_at")
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @OneToOne(mappedBy = "merchant", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private MerchantRiskConfig riskConfig;
}
