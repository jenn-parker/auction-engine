package com.jennparker.auction;

import com.jennparker.auction.application.CustomerInvoiceService;
import com.jennparker.auction.domain.Order;
import com.jennparker.auction.persistence.CustomerRepository;
import com.jennparker.auction.persistence.OrderEntity;
import com.jennparker.auction.persistence.OrderRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class CustomerInvoiceServiceTest {
    @Test
    void itemizesPaidAndUnpaidWonLots() {
        CustomerRepository customers = mock(CustomerRepository.class);
        OrderRepository orders = mock(OrderRepository.class);
        OrderEntity paid = mock(OrderEntity.class);
        OrderEntity unpaid = mock(OrderEntity.class);
        when(customers.existsById("cust-1")).thenReturn(true);
        when(orders.findByCustomerIdOrderByCreatedAtAsc("cust-1")).thenReturn(Arrays.asList(paid, unpaid));
        when(paid.toDomain()).thenReturn(new Order("order-1", "lot-1", "cust-1", BigDecimal.valueOf(100), Instant.EPOCH, Order.Status.SETTLED));
        when(unpaid.toDomain()).thenReturn(new Order("order-2", "lot-2", "cust-1", BigDecimal.valueOf(25), Instant.EPOCH, Order.Status.PENDING));

        CustomerInvoiceService.Invoice invoice = new CustomerInvoiceService(customers, orders).getInvoice("cust-1");

        assertEquals(2, invoice.getLines().size());
        assertEquals(0, new BigDecimal("125").compareTo(invoice.getTotalCost()));
        assertEquals(0, new BigDecimal("100").compareTo(invoice.getAmountPaid()));
        assertEquals(0, new BigDecimal("25").compareTo(invoice.getAmountOwed()));
        assertEquals(Order.Status.SETTLED, invoice.getLines().get(0).getPaymentStatus());
        assertEquals(Order.Status.PENDING, invoice.getLines().get(1).getPaymentStatus());
    }
}
