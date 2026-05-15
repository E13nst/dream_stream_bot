package com.example.dream_stream_bot.service.payment;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReceiptEmailAwaitHolderTest {

    private static final class MutableClock extends Clock {
        private Instant now;

        private MutableClock(Instant start) {
            this.now = start;
        }

        void advance(Duration d) {
            now = now.plus(d);
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    @Test
    void startPersonalThenOverwriteWithGroup() {
        MutableClock clock = new MutableClock(Instant.parse("2025-06-01T12:00:00Z"));
        ReceiptEmailAwaitHolder h = new ReceiptEmailAwaitHolder(clock);
        h.startAwaitingPersonal(1L, 2L, 99L);
        ReceiptEmailAwaitHolder.Session s1 = h.get(1L, 2L).orElseThrow();
        assertEquals(ReceiptEmailAwaitHolder.Kind.PERSONAL_TARIFF, s1.getKind());
        assertEquals(99L, s1.getPersonalTariffId());

        h.startAwaitingGroup(1L, 2L, 42L);
        ReceiptEmailAwaitHolder.Session s2 = h.get(1L, 2L).orElseThrow();
        assertEquals(ReceiptEmailAwaitHolder.Kind.GROUP_SUBSCRIPTION, s2.getKind());
        assertEquals(42L, s2.getGroupSubscriptionId());
    }

    @Test
    void clearRemovesSession() {
        ReceiptEmailAwaitHolder h = new ReceiptEmailAwaitHolder();
        h.startAwaitingPersonal(3L, 4L, 1L);
        assertTrue(h.get(3L, 4L).isPresent());
        h.clear(3L, 4L);
        assertTrue(h.get(3L, 4L).isEmpty());
    }

    @Test
    void isExpiredAfterTtl() {
        MutableClock clock = new MutableClock(Instant.parse("2025-06-01T12:00:00Z"));
        ReceiptEmailAwaitHolder h = new ReceiptEmailAwaitHolder(clock);
        h.startAwaitingPersonal(1L, 1L, 5L);
        ReceiptEmailAwaitHolder.Session s = h.get(1L, 1L).orElseThrow();
        assertFalse(h.isExpired(s));
        clock.advance(ReceiptEmailAwaitHolder.DEFAULT_TTL.plusSeconds(1));
        assertTrue(h.isExpired(s));
    }
}
