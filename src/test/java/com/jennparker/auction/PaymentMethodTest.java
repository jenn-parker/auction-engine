package com.jennparker.auction;

import com.jennparker.auction.domain.PaymentMethod;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PaymentMethodTest {
    @Test
    void retainsProviderTokenAndCardExpirationMetadata() {
        PaymentMethod method = new PaymentMethod("pm_card_123", "stripe", PaymentMethod.Type.CARD, true, false, 12, 2030);

        assertEquals("pm_card_123", method.getId());
        assertEquals("stripe", method.getProvider());
        assertEquals(12, method.getExpirationMonth());
        assertEquals(2030, method.getExpirationYear());
    }

    @Test
    void rejectsInvalidExpirationMetadata() {
        assertThrows(IllegalArgumentException.class,
                () -> new PaymentMethod("pm-card", "stripe", PaymentMethod.Type.CARD, true, false, 13, 2030));
        assertThrows(IllegalArgumentException.class,
                () -> new PaymentMethod("pm-card", "stripe", PaymentMethod.Type.CARD, true, false, 1, 1999));
    }
}
