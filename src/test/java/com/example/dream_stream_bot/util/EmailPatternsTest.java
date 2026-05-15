package com.example.dream_stream_bot.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmailPatternsTest {

    @Test
    void acceptsTypicalEmail() {
        assertTrue(EmailPatterns.isValidBillingEmail("E13.nst@gmail.com"));
        assertTrue(EmailPatterns.isValidBillingEmail("  user+tag@example.co.uk  "));
    }

    @Test
    void rejectsNonEmail() {
        assertFalse(EmailPatterns.isValidBillingEmail("not an email"));
        assertFalse(EmailPatterns.isValidBillingEmail("a@b")); // TLD too short
        assertFalse(EmailPatterns.isValidBillingEmail(""));
        assertFalse(EmailPatterns.isValidBillingEmail(null));
    }
}
