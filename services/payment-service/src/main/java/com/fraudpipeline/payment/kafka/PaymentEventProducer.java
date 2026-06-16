package com.fraudpipeline.payment.kafka;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class PaymentEventProducer {

    private static final String TOPIC_SETTLED = "payment.settled";
    private static final String TOPIC_FLAGGED = "payment.flagged";

    private final KafkaTemplate<String, String> kafkaTemplate;

    public PaymentEventProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishSettled(String paymentId, String eventJson) {
        kafkaTemplate.send(TOPIC_SETTLED, paymentId, eventJson);
    }

    public void publishFlagged(String paymentId, String eventJson) {
        kafkaTemplate.send(TOPIC_FLAGGED, paymentId, eventJson);
    }
}
