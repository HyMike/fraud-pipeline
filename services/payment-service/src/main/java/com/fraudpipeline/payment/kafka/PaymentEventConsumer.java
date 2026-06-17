package com.fraudpipeline.payment.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventConsumer.class);

    @KafkaListener(topics = "payment.flagged", groupId = "payment-service")
    public void onPaymentFlagged(String message) {
        log.info("Payment flagged event received: {}", message);
    }

    @KafkaListener(topics = "payment.settled", groupId = "payment-service")
    public void onPaymentSettled(String message) {
        log.info("Payment settled event received: {}", message);
    }
}
