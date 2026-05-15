package com.example.dream_stream_bot.util;

import java.util.regex.Pattern;

/**
 * Единый паттерн email для чека ЮKassa и команд бота.
 */
public final class EmailPatterns {

    private static final Pattern BILLING_EMAIL = Pattern.compile(
            "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,}$");

    private EmailPatterns() {
    }

    public static boolean isValidBillingEmail(String raw) {
        if (raw == null) {
            return false;
        }
        String t = raw.trim();
        return !t.isEmpty() && BILLING_EMAIL.matcher(t).matches();
    }
}
