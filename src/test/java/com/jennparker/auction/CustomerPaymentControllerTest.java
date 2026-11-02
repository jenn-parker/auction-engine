package com.jennparker.auction;

import com.jennparker.auction.api.ApiExceptionHandler;
import com.jennparker.auction.api.PaymentController;
import com.jennparker.auction.application.PaymentService;
import com.jennparker.auction.application.PaymentQueryService;
import com.jennparker.auction.domain.Payment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CustomerPaymentControllerTest {
    private MockMvc mvc;
    private PaymentQueryService payments;
    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        payments = mock(PaymentQueryService.class);
        paymentService = mock(PaymentService.class);
        mvc = MockMvcBuilders.standaloneSetup(new PaymentController(payments, paymentService))
                .setControllerAdvice(new ApiExceptionHandler()).build();
    }

    @Test
    void listsCustomerPayments() throws Exception {
        when(payments.list("cust-1")).thenReturn(Collections.singletonList(
                new Payment("pay-1", "cust-1", "pm-1", BigDecimal.TEN, "APPLIED", Instant.parse("2026-11-01T20:00:00Z"))));

        mvc.perform(get("/api/v1/customers/cust-1/payments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("pay-1"))
                .andExpect(jsonPath("$[0].paymentMethodId").value("pm-1"));
    }

    @Test
    void createsAnAuthorizedPaymentWithAnIdempotencyKey() throws Exception {
        when(paymentService.create(eq("cust-1"), eq("pm-1"), any(BigDecimal.class), eq("payment-request-1")))
                .thenReturn(new Payment("pay-1", "cust-1", "pm-1", BigDecimal.TEN, "AUTHORIZED", Instant.parse("2026-11-01T20:00:00Z")));

        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/payments")
                        .header("Idempotency-Key", "payment-request-1")
                        .contentType("application/json")
                        .content("{\"customerId\":\"cust-1\",\"paymentMethodId\":\"pm-1\",\"amount\":10.00}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/payments/pay-1"))
                .andExpect(jsonPath("$.status").value("AUTHORIZED"));
    }
}
