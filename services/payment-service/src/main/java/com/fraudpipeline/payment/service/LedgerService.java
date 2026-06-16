package com.fraudpipeline.payment.service;

import com.fraudpipeline.payment.entity.JournalEntry;
import com.fraudpipeline.payment.entity.Account;
import com.fraudpipeline.payment.entity.Payment;
import com.fraudpipeline.payment.repository.AccountRepository;
import com.fraudpipeline.payment.repository.JournalEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LedgerService {

    private final AccountRepository accountRepository;
    private final JournalEntryRepository journalEntryRepository;

    @Transactional
    public void settle(Payment payment) {
        Account customerFunds = accountRepository.findByName("Customer Funds")
                .orElseThrow(() -> new IllegalStateException("Account 'Customer Funds' not found"));
        Account merchantPayable = accountRepository.findByName("Merchant Payable")
                .orElseThrow(() -> new IllegalStateException("Account 'Merchant Payable' not found"));

        JournalEntry debit = JournalEntry.builder()
                .payment(payment)
                .account(customerFunds)
                .direction(JournalEntry.Direction.DEBIT)
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .build();

        JournalEntry credit = JournalEntry.builder()
                .payment(payment)
                .account(merchantPayable)
                .direction(JournalEntry.Direction.CREDIT)
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .build();

        journalEntryRepository.save(debit);
        journalEntryRepository.save(credit);
    }
}
