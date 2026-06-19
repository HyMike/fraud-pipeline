package com.fraudpipeline.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fraudpipeline.payment.client.FraudScoringClient;
import com.fraudpipeline.payment.entity.*;
import com.fraudpipeline.payment.kafka.PaymentEventProducer;
import com.fraudpipeline.payment.repository.CaseRepository;
import com.fraudpipeline.payment.repository.MerchantRepository;
import com.fraudpipeline.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final MerchantRepository merchantRepository;
    private final CaseRepository caseRepository;
    private final IdempotencyService idempotencyService;
    private final FraudScoringClient fraudScoringClient;
    private final LedgerService ledgerService;
    private final WebhookService webhookService;
    private final PaymentEventProducer eventProducer;
    private final ObjectMapper objectMapper;

    @Transactional
    public PaymentResult process(PaymentRequest request) throws Exception {
        Merchant merchant = merchantRepository.findByApiKey(request.apiKey())
                .orElseThrow(() -> new SecurityException("Invalid API key"));

        // 1. Idempotency check
        Optional<String> cached = idempotencyService.getCachedResponse(
                merchant.getId().toString(), request.idempotencyKey());
        if (cached.isPresent()) {
            return objectMapper.readValue(cached.get(), PaymentResult.class);
        }

        // 2. Create payment record
        Payment payment = Payment.builder()
                .merchant(merchant)
                .amount(request.amount())
                .currency(request.currency())
                .idempotencyKey(request.idempotencyKey())
                .build();
        payment = paymentRepository.save(payment);

        // 3. Score for fraud
        Map<String, Object> features = Map.of(
                "TransactionAmt", request.amount().doubleValue()
        );
        FraudScoringClient.ScoreResponse scoreResponse = fraudScoringClient.score(features);

        payment.setFraudScore(BigDecimal.valueOf(scoreResponse.score()));
        payment.setStatus(Payment.Status.FRAUD_SCORED);

        // 4. Get merchant thresholds
        MerchantRiskConfig config = merchant.getRiskConfig();
        double approveBelow = config != null ? config.getAutoApproveBelow().doubleValue() : 0.30;
        double blockAbove   = config != null ? config.getAutoBlockAbove().doubleValue()   : 0.90;

        // 5. Route based on score
        PaymentResult result;
        String callbackUrl = merchant.getCallbackUrl();

        if (scoreResponse.score() < approveBelow) {
            payment.setStatus(Payment.Status.SETTLED);
            paymentRepository.save(payment);
            ledgerService.settle(payment);
            result = new PaymentResult(payment.getId(), "SETTLED", scoreResponse.score(), null);
            eventProducer.publishSettled(payment.getId().toString(),
                    objectMapper.writeValueAsString(result));
            if (callbackUrl != null) {
                webhookService.fire(callbackUrl, payment.getId().toString(), "SETTLED");
            }

        } else if (scoreResponse.score() >= blockAbove) {
            payment.setStatus(Payment.Status.BLOCKED);
            paymentRepository.save(payment);
            result = new PaymentResult(payment.getId(), "BLOCKED", scoreResponse.score(), null);
            if (callbackUrl != null) {
                webhookService.fire(callbackUrl, payment.getId().toString(), "BLOCKED");
            }

        } else {
            payment.setStatus(Payment.Status.FLAGGED);
            paymentRepository.save(payment);

            Case fraudCase = Case.builder()
                    .payment(payment)
                    .fraudScore(BigDecimal.valueOf(scoreResponse.score()))
                    .shapValues(objectMapper.writeValueAsString(scoreResponse.shap_values()))
                    .build();
            caseRepository.save(fraudCase);

            result = new PaymentResult(payment.getId(), "FLAGGED", scoreResponse.score(),
                    fraudCase.getId());
            eventProducer.publishFlagged(payment.getId().toString(),
                    objectMapper.writeValueAsString(result));
        }

        // 6. Cache for idempotency
        idempotencyService.cacheResponse(merchant.getId().toString(), request.idempotencyKey(),
                objectMapper.writeValueAsString(result));

        return result;
    }

    public record PaymentRequest(
            String apiKey,
            String idempotencyKey,
            BigDecimal amount,
            String currency
    ) {}

    public record PaymentResult(
            UUID paymentId,
            String status,
            double fraudScore,
            UUID caseId
    ) {}
}
