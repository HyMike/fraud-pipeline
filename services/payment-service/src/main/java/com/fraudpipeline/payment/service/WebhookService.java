package com.fraudpipeline.payment.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Service
public class WebhookService {

    private static final Logger log = LoggerFactory.getLogger(WebhookService.class);

    private final WebClient webClient;

    public WebhookService(WebClient.Builder builder) {
        this.webClient = builder.build();
    }

    @Async
    @Retryable(
        retryFor = Exception.class,
        maxAttempts = 5,
        backoff = @Backoff(delay = 1000, multiplier = 2)
        // retries at: 1s, 2s, 4s, 8s, 16s
    )
    public void fire(String callbackUrl, String paymentId, String status) {
        log.info("Firing webhook to {} for payment {} status {}", callbackUrl, paymentId, status);

        Map<String, String> payload = Map.of(
                "paymentId", paymentId,
                "status", status
        );

        webClient.post()
                .uri(callbackUrl)
                .bodyValue(payload)
                .retrieve()
                .toBodilessEntity()
                .block();

        log.info("Webhook delivered successfully to {} for payment {}", callbackUrl, paymentId);
    }
}
