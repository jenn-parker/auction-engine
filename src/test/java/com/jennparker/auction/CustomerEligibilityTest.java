package com.jennparker.auction;

import com.jennparker.auction.domain.Customer;
import com.jennparker.auction.domain.PaymentMethod;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class CustomerEligibilityTest {
    @Test
    void anyAuthorizedPaymentMethodMakesAnActiveBidderEligible() {
        Customer customer = new Customer("cust-1", "Jenn Parker", "jenn@example.com", "555-0100", null,
                Customer.AccountStatus.ACTIVE, true, Arrays.asList(
                new PaymentMethod("pm-1", PaymentMethod.Type.CARD, false),
                new PaymentMethod("pm-2", PaymentMethod.Type.WALLET, true, true)));

        assertTrue(customer.isEligibleToBid());
        assertEquals(2, customer.getPaymentMethods().size());
        assertThrows(UnsupportedOperationException.class,
                () -> customer.getPaymentMethods().add(new PaymentMethod("pm-3", PaymentMethod.Type.CARD, true)));
    }

    @Test
    void suspendedOrDisabledCustomersCannotBid() {
        PaymentMethod authorized = new PaymentMethod("pm-1", PaymentMethod.Type.CARD, true);
        assertFalse(new Customer("cust-1", Customer.AccountStatus.SUSPENDED, true, authorized).isEligibleToBid());
        assertFalse(new Customer("cust-2", Customer.AccountStatus.ACTIVE, false, authorized).isEligibleToBid());
        assertFalse(new Customer("cust-3", Customer.AccountStatus.ACTIVE, true,
                new PaymentMethod("pm-2", PaymentMethod.Type.CARD, false)).isEligibleToBid());
    }
}
