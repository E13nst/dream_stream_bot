package com.example.dream_stream_bot.bot.command;

import com.example.dream_stream_bot.bot.message.OutgoingMessage;

import java.util.Collections;
import java.util.List;

/**
 * Обработчик inline-кнопок (callback_query).
 *
 * <p>Активируется по префиксу {@link #prefix()} в {@code callback_data}.
 * Например, для {@code callback_data = "consent_accept:42"} префикс — {@code "consent_accept"}.</p>
 */
public interface CallbackHandler {

    String prefix();

    List<OutgoingMessage> handle(CallbackContext ctx);

    static List<OutgoingMessage> silent() {
        return Collections.emptyList();
    }
}
