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
public class CustomerBalanceService {
    private final CustomerRepository customers;
    private final OrderRepository orders;

    public CustomerBalanceService(CustomerRepository customers, OrderRepository orders) {
        this.customers = customers; this.orders = orders;
    }

    @Transactional(readOnly = true)
    public Balance getBalance(String customerId) {
        if (!customers.existsById(customerId)) throw new CustomerManagementService.CustomerNotFoundException();
        List<Order> unpaid = orders.findByCustomerIdAndStatusNotOrderByCreatedAtAsc(customerId, Order.Status.SETTLED.name())
                .stream().map(entity -> entity.toDomain()).collect(Collectors.toList());
        BigDecimal total = unpaid.stream().map(Order::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new Balance(customerId, total, unpaid);
    }

    public static final class Balance {
        private final String customerId;
        private final BigDecimal totalAmountDue;
        private final List<Order> orders;

        public Balance(String customerId, BigDecimal totalAmountDue, List<Order> orders) {
            this.customerId = customerId; this.totalAmountDue = totalAmountDue;
            this.orders = Collections.unmodifiableList(orders);
        }
        public String getCustomerId() { return customerId; }
        public BigDecimal getTotalAmountDue() { return totalAmountDue; }
        public List<Order> getOrders() { return orders; }
    }
}
