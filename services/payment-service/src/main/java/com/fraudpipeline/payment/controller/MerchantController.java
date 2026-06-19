package com.fraudpipeline.payment.controller;

import com.fraudpipeline.payment.entity.Merchant;
import com.fraudpipeline.payment.entity.MerchantRiskConfig;
import com.fraudpipeline.payment.repository.MerchantRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/merchants")
@RequiredArgsConstructor
public class MerchantController {

    private final MerchantRepository merchantRepository;

    @PostMapping
    public ResponseEntity<Merchant> register(@Valid @RequestBody RegisterRequest body) {
        Merchant merchant = Merchant.builder()
                .name(body.name())
                .apiKey(body.apiKey())
                .callbackUrl(body.callbackUrl())
                .build();

        MerchantRiskConfig config = MerchantRiskConfig.builder()
                .merchant(merchant)
                .autoApproveBelow(new BigDecimal("0.30"))
                .reviewAbove(new BigDecimal("0.50"))
                .autoBlockAbove(new BigDecimal("0.90"))
                .build();

        merchant.setRiskConfig(config);
        return ResponseEntity.ok(merchantRepository.save(merchant));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Merchant> get(@PathVariable UUID id) {
        return merchantRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    public record RegisterRequest(
            @NotBlank(message = "Name is required")
            String name,

            @NotBlank(message = "API key is required")
            String apiKey,

            String callbackUrl
    ) {}
}
