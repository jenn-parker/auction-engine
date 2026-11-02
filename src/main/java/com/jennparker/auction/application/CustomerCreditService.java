package com.jennparker.auction.application;

import com.jennparker.auction.persistence.CreditLedgerEntity;
import com.jennparker.auction.persistence.CreditLedgerRepository;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;

@Service
public class CustomerCreditService {
    private final CreditLedgerRepository ledger;
    public CustomerCreditService(CreditLedgerRepository ledger) { this.ledger = ledger; }
    public BigDecimal balance(String customerId) {
        return ledger.findAllByCustomerId(customerId).stream().map(CreditLedgerEntity::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    public void consume(String customerId, BigDecimal amount, String reference) {
        if (amount.signum() > 0) {
            if (balance(customerId).compareTo(amount) < 0) throw new AuctionService.PaymentConflictException();
            ledger.save(new CreditLedgerEntity(customerId, amount.negate(), "CREDIT_APPLIED", reference));
        }
    }
    public void grant(String customerId, BigDecimal amount, String reference) {
        if (amount.signum() > 0) ledger.save(new CreditLedgerEntity(customerId, amount, "OVERPAYMENT_CREDIT", reference));
    }
}
