package com.fraudpipeline.payment.service;

import com.fraudpipeline.payment.entity.Case;
import com.fraudpipeline.payment.entity.Merchant;
import com.fraudpipeline.payment.entity.Payment;
import com.fraudpipeline.payment.repository.CaseRepository;
import com.fraudpipeline.payment.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CaseServiceTest {

    @Mock CaseRepository caseRepository;
    @Mock PaymentRepository paymentRepository;
    @Mock LedgerService ledgerService;
    @Mock WebhookService webhookService;

    @InjectMocks CaseService caseService;

    @Test
    void approveSettlesLedgerAndUpdatesStatus() {
        Merchant merchant = Merchant.builder()
                .name("Test Merchant")
                .apiKey("key-123")
                .build();

        Payment payment = Payment.builder()
                .merchant(merchant)
                .amount(new BigDecimal("250.00"))
                .currency("USD")
                .idempotencyKey("order-1")
                .build();

        Case fraudCase = Case.builder()
                .payment(payment)
                .fraudScore(new BigDecimal("0.65"))
                .shapValues("[]")
                .build();

        when(caseRepository.findById(any())).thenReturn(Optional.of(fraudCase));
        when(caseRepository.save(any())).thenReturn(fraudCase);

        CaseService.DecisionRequest request = new CaseService.DecisionRequest(
                CaseService.Decision.APPROVE, "analyst-1", "Looks legitimate"
        );

        Case result = caseService.decide(UUID.randomUUID(), request);

        assertThat(result.getStatus()).isEqualTo(Case.Status.APPROVED);
        assertThat(result.getAnalystId()).isEqualTo("analyst-1");
        verify(ledgerService).settle(payment);
    }

    @Test
    void blockDoesNotSettleLedger() {
        Merchant merchant = Merchant.builder()
                .name("Test Merchant")
                .apiKey("key-123")
                .build();

        Payment payment = Payment.builder()
                .merchant(merchant)
                .amount(new BigDecimal("250.00"))
                .currency("USD")
                .idempotencyKey("order-1")
                .build();

        Case fraudCase = Case.builder()
                .payment(payment)
                .fraudScore(new BigDecimal("0.85"))
                .shapValues("[]")
                .build();

        when(caseRepository.findById(any())).thenReturn(Optional.of(fraudCase));
        when(caseRepository.save(any())).thenReturn(fraudCase);

        CaseService.DecisionRequest request = new CaseService.DecisionRequest(
                CaseService.Decision.BLOCK, "analyst-1", "Confirmed fraud"
        );

        Case result = caseService.decide(UUID.randomUUID(), request);

        assertThat(result.getStatus()).isEqualTo(Case.Status.BLOCKED);
        verify(ledgerService, never()).settle(any());
    }

    @Test
    void throwsWhenCaseAlreadyDecided() {
        Case fraudCase = Case.builder()
                .payment(Payment.builder()
                        .merchant(Merchant.builder().name("m").apiKey("k").build())
                        .amount(BigDecimal.TEN)
                        .currency("USD")
                        .idempotencyKey("k1")
                        .build())
                .fraudScore(new BigDecimal("0.65"))
                .shapValues("[]")
                .status(Case.Status.APPROVED)
                .build();

        when(caseRepository.findById(any())).thenReturn(Optional.of(fraudCase));

        assertThatThrownBy(() -> caseService.decide(UUID.randomUUID(),
                new CaseService.DecisionRequest(CaseService.Decision.BLOCK, "analyst-1", "")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already decided");
    }

    @Test
    void throwsWhenCaseNotFound() {
        when(caseRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> caseService.decide(UUID.randomUUID(),
                new CaseService.DecisionRequest(CaseService.Decision.APPROVE, "analyst-1", "")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Case not found");
    }
}
