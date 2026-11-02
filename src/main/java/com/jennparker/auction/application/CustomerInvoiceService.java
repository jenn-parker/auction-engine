package com.jennparker.auction.application;

import com.jennparker.auction.domain.Order;
import com.jennparker.auction.persistence.CustomerRepository;
import com.jennparker.auction.persistence.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CustomerInvoiceService {
    private final CustomerRepository customers;
    private final OrderRepository orders;

    public CustomerInvoiceService(CustomerRepository customers, OrderRepository orders) {
        this.customers = customers; this.orders = orders;
    }

    @Transactional(readOnly = true)
    public Invoice getInvoice(String customerId) {
        if (!customers.existsById(customerId)) throw new CustomerManagementService.CustomerNotFoundException();
        List<InvoiceLine> lines = orders.findByCustomerIdOrderByCreatedAtAsc(customerId).stream()
                .map(entity -> new InvoiceLine(entity.toDomain())).collect(Collectors.toList());
        BigDecimal totalCost = lines.stream().map(InvoiceLine::getCost).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal amountPaid = lines.stream().map(InvoiceLine::getAmountPaid).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal amountOwed = lines.stream().map(InvoiceLine::getAmountOwed).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new Invoice(customerId, lines, totalCost, amountPaid, amountOwed);
    }

    public static final class Invoice {
        private final String customerId;
        private final List<InvoiceLine> lines;
        private final BigDecimal totalCost;
        private final BigDecimal amountPaid;
        private final BigDecimal amountOwed;

        public Invoice(String customerId, List<InvoiceLine> lines, BigDecimal totalCost, BigDecimal amountPaid, BigDecimal amountOwed) {
            this.customerId = customerId; this.lines = Collections.unmodifiableList(lines);
            this.totalCost = totalCost; this.amountPaid = amountPaid; this.amountOwed = amountOwed;
        }
        public String getCustomerId() { return customerId; }
        public List<InvoiceLine> getLines() { return lines; }
        public BigDecimal getTotalCost() { return totalCost; }
        public BigDecimal getAmountPaid() { return amountPaid; }
        public BigDecimal getAmountOwed() { return amountOwed; }
    }

    public static final class InvoiceLine {
        private final String orderId;
        private final String lotId;
        private final BigDecimal cost;
        private final Order.Status paymentStatus;
        private final BigDecimal amountPaid;
        private final BigDecimal amountOwed;

        public InvoiceLine(Order order) {
            this.orderId = order.getId(); this.lotId = order.getLotId(); this.cost = order.getAmount();
            this.paymentStatus = order.getStatus();
            this.amountPaid = order.getStatus() == Order.Status.SETTLED ? order.getAmount() : BigDecimal.ZERO;
            this.amountOwed = order.getStatus() == Order.Status.SETTLED ? BigDecimal.ZERO : order.getAmount();
        }
        public String getOrderId() { return orderId; }
        public String getLotId() { return lotId; }
        public BigDecimal getCost() { return cost; }
        public Order.Status getPaymentStatus() { return paymentStatus; }
        public BigDecimal getAmountPaid() { return amountPaid; }
        public BigDecimal getAmountOwed() { return amountOwed; }
    }
}
