package com.fraudpipeline.payment.service;

import com.fraudpipeline.payment.entity.Case;
import com.fraudpipeline.payment.entity.Payment;
import com.fraudpipeline.payment.repository.CaseRepository;
import com.fraudpipeline.payment.repository.PaymentRepository;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CaseService {

    private final CaseRepository caseRepository;
    private final PaymentRepository paymentRepository;
    private final LedgerService ledgerService;
    private final WebhookService webhookService;

    public Page<Case> getCases(Case.Status status, Pageable pageable) {
        if (status != null) {
            return caseRepository.findByStatus(status, pageable);
        }
        return caseRepository.findAll(pageable);
    }

    public Case getCase(UUID id) {
        return caseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Case not found: " + id));
    }

    @Transactional
    public Case decide(UUID caseId, DecisionRequest request) {
        Case fraudCase = caseRepository.findById(caseId)
                .orElseThrow(() -> new IllegalArgumentException("Case not found: " + caseId));

        if (fraudCase.getStatus() != Case.Status.PENDING) {
            throw new IllegalStateException("Case already decided: " + fraudCase.getStatus());
        }

        Payment payment = fraudCase.getPayment();

        if (request.decision() == Decision.APPROVE) {
            ledgerService.settle(payment);
            payment.setStatus(Payment.Status.APPROVED);
            fraudCase.setStatus(Case.Status.APPROVED);
        } else {
            payment.setStatus(Payment.Status.BLOCKED);
            fraudCase.setStatus(Case.Status.BLOCKED);
        }

        fraudCase.setAnalystId(request.analystId());
        fraudCase.setNotes(request.notes());
        fraudCase.setDecisionAt(OffsetDateTime.now());

        paymentRepository.save(payment);
        Case saved = caseRepository.save(fraudCase);

        // Fire webhook asynchronously after decision
        String callbackUrl = payment.getMerchant().getCallbackUrl();
        if (callbackUrl != null) {
            webhookService.fire(callbackUrl, payment.getId().toString(), saved.getStatus().name());
        }

        return saved;
    }

    public enum Decision { APPROVE, BLOCK }

    public record DecisionRequest(
            @NotNull(message = "Decision is required")
            Decision decision,

            @NotBlank(message = "Analyst ID is required")
            String analystId,

            String notes
    ) {}
}
