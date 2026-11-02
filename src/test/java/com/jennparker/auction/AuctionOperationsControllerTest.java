package com.jennparker.auction;

import com.jennparker.auction.api.ApiExceptionHandler;
import com.jennparker.auction.api.AuctionOperationsController;
import com.jennparker.auction.application.AuctionManagementService;
import com.jennparker.auction.application.AuctionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuctionOperationsControllerTest {
    private MockMvc mvc;
    private AuctionService auctions;

    @BeforeEach
    void setUp() {
        auctions = mock(AuctionService.class);
        mvc = MockMvcBuilders.standaloneSetup(new AuctionOperationsController(auctions, mock(AuctionManagementService.class)))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void acceptsAValidBidRequest() throws Exception {
        mvc.perform(post("/api/v1/lots/lot-1/bids")
                        .contentType("application/json")
                        .content("{\"customerId\":\"cust-1\",\"bidAmount\":125.00,\"timestamp\":\"2026-11-01T20:14:32Z\"}"))
                .andExpect(status().isAccepted());

        verify(auctions).bid(eq("lot-1"), any());
    }

    @Test
    void rejectsInvalidBidPayloadAtTheApiBoundary() throws Exception {
        mvc.perform(post("/api/v1/lots/lot-1/bids")
                        .contentType("application/json")
                        .content("{\"customerId\":\"\",\"bidAmount\":0,\"timestamp\":null}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(auctions);
    }

    @Test
    void exposesLotSettlementAsAValidatedPaymentRequest() throws Exception {
        when(auctions.settle(eq("lot-1"), any())).thenReturn(
                new com.jennparker.auction.domain.Order("lot-1", "cust-1", java.math.BigDecimal.valueOf(125), java.time.Instant.parse("2026-11-01T20:00:00Z")));
        mvc.perform(post("/api/v1/internal/lots/lot-1/settle")
                        .contentType("application/json")
                        .content("{\"customerId\":\"cust-1\",\"payments\":[{\"paymentMethodId\":\"pm-1\",\"amount\":125.00}],\"creditAmount\":0}"))
                .andExpect(status().isCreated());
    }
}
