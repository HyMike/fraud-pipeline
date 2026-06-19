package com.fraudpipeline.payment.controller;

import com.fraudpipeline.payment.service.PaymentService;
import com.fraudpipeline.payment.service.PaymentService.PaymentRequest;
import com.fraudpipeline.payment.service.PaymentService.PaymentResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<PaymentResult> createPayment(
            @RequestHeader("X-API-Key") String apiKey,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody PaymentRequestBody body) throws Exception {

        PaymentRequest request = new PaymentRequest(
                apiKey,
                idempotencyKey,
                body.amount(),
                body.currency() != null ? body.currency() : "USD"
        );

        PaymentResult result = paymentService.process(request);

        HttpStatus status = switch (result.status()) {
            case "SETTLED" -> HttpStatus.OK;
            case "FLAGGED" -> HttpStatus.ACCEPTED;
            case "BLOCKED" -> HttpStatus.UNPROCESSABLE_ENTITY;
            default -> HttpStatus.OK;
        };

        return ResponseEntity.status(status).body(result);
    }

    public record PaymentRequestBody(
            @NotNull(message = "Amount is required")
            @Positive(message = "Amount must be greater than zero")
            BigDecimal amount,

            String currency
    ) {}
}
