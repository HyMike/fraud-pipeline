package com.fraudpipeline.payment.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Component
public class FraudScoringClient {

    private final WebClient webClient;

    public FraudScoringClient(@Value("${fraud.scoring.url}") String baseUrl) {
        this.webClient = WebClient.builder().baseUrl(baseUrl).build();
    }

    public ScoreResponse score(Map<String, Object> features) {
        return webClient.post()
                .uri("/score")
                .bodyValue(features)
                .retrieve()
                .bodyToMono(ScoreResponse.class)
                .block();
    }

    public record ScoreResponse(
            double score,
            String risk_level,
            List<ShapValue> shap_values
    ) {}

    public record ShapValue(
            String feature,
            double value,
            double contribution
    ) {}
}
