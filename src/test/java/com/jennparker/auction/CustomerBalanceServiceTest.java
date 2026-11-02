package com.jennparker.auction;

import com.jennparker.auction.application.CustomerBalanceService;
import com.jennparker.auction.domain.Order;
import com.jennparker.auction.persistence.CustomerRepository;
import com.jennparker.auction.persistence.OrderEntity;
import com.jennparker.auction.persistence.OrderRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CustomerBalanceServiceTest {
    @Test
    void totalsOnlyUnsettledOrders() {
        CustomerRepository customers = mock(CustomerRepository.class);
        OrderRepository orders = mock(OrderRepository.class);
        OrderEntity first = mock(OrderEntity.class);
        OrderEntity second = mock(OrderEntity.class);
        when(customers.existsById("cust-1")).thenReturn(true);
        when(orders.findByCustomerIdAndStatusNotOrderByCreatedAtAsc("cust-1", "SETTLED"))
                .thenReturn(Arrays.asList(first, second));
        when(first.toDomain()).thenReturn(new Order("lot-1", "cust-1", BigDecimal.valueOf(100), Instant.EPOCH));
        when(second.toDomain()).thenReturn(new Order("lot-2", "cust-1", BigDecimal.valueOf(25.50), Instant.EPOCH));

        CustomerBalanceService.Balance balance = new CustomerBalanceService(customers, orders).getBalance("cust-1");

        assertEquals(0, new BigDecimal("125.50").compareTo(balance.getTotalAmountDue()));
        assertEquals(2, balance.getOrders().size());
    }
}
