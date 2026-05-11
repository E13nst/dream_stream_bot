package com.example.dream_stream_bot.service.access;

import com.example.dream_stream_bot.model.telegram.BotEntity;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.util.Locale;

/**
 * Решает, адресовано ли сообщение боту в групповом чате.
 * Сообщение считается адресованным, если:
 *  - это reply на сообщение бота;
 *  - текст содержит {@code @bot_username};
 *  - текст содержит имя бота, его alias или триггер-слово (например, {@code #сон}).
 */
@Component
public class GroupTriggerMatcher {

    public boolean isAddressedToBot(BotEntity bot, Message message, String botUsername) {
        if (message == null || message.getText() == null) {
            return false;
        }
        if (isReplyToBot(message, botUsername)) {
            return true;
        }
        String lowerText = message.getText().toLowerCase(Locale.ROOT);
        if (botUsername != null && !botUsername.isBlank()
                && lowerText.contains("@" + botUsername.toLowerCase(Locale.ROOT))) {
            return true;
        }
        if (bot == null) {
            return false;
        }
        if (containsCaseInsensitive(lowerText, bot.getName())) {
            return true;
        }
        for (String alias : bot.getBotAliasesList()) {
            if (containsCaseInsensitive(lowerText, alias)) {
                return true;
            }
        }
        for (String trigger : bot.getBotTriggersList()) {
            if (containsCaseInsensitive(lowerText, trigger)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isReplyToBot(Message message, String botUsername) {
        if (message.getReplyToMessage() == null || message.getReplyToMessage().getFrom() == null) {
            return false;
        }
        String fromUsername = message.getReplyToMessage().getFrom().getUserName();
        return fromUsername != null && fromUsername.equalsIgnoreCase(botUsername);
    }

    private static boolean containsCaseInsensitive(String lowerText, String needle) {
        return needle != null && !needle.isEmpty()
                && lowerText.contains(needle.toLowerCase(Locale.ROOT));
    }
}
