package com.jennparker.auction.application;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

public final class SettlementPaymentPlan {
    private final List<Payment> payments;
    private final BigDecimal creditAmount;
    private final String customerId;
    public SettlementPaymentPlan(List<Payment> payments, BigDecimal creditAmount) { this(null, payments, creditAmount); }
    public SettlementPaymentPlan(String customerId, List<Payment> payments, BigDecimal creditAmount) {
        this.customerId = customerId;
        this.payments = Collections.unmodifiableList(payments == null ? Collections.<Payment>emptyList() : payments);
        this.creditAmount = creditAmount == null ? BigDecimal.ZERO : creditAmount;
    }
    public List<Payment> getPayments() { return payments; }
    public BigDecimal getCreditAmount() { return creditAmount; }
    public String getCustomerId() { return customerId; }
    public BigDecimal externalTotal() { return payments.stream().map(Payment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add); }
    public BigDecimal total() { return externalTotal().add(creditAmount); }
    public static final class Payment {
        private final String paymentMethodId;
        private final String paymentId;
        private final BigDecimal amount;
        public Payment(String paymentMethodId, BigDecimal amount) { this(null, paymentMethodId, amount); }
        public Payment(String paymentId, String paymentMethodId, BigDecimal amount) { this.paymentId = paymentId; this.paymentMethodId = paymentMethodId; this.amount = amount; }
        public String getPaymentId() { return paymentId; }
        public String getPaymentMethodId() { return paymentMethodId; }
        public BigDecimal getAmount() { return amount; }
    }
}
