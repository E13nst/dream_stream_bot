package com.example.dream_stream_bot.bot.message;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.ActionType;
import org.telegram.telegrambots.meta.api.methods.send.SendChatAction;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

/**
 * Единая точка отправки сообщений в Telegram. Проксирует {@link OutgoingMessage}
 * через {@link AbsSender} с правильным проставлением {@code message_thread_id}
 * (форум-топики) и {@code reply_to_message_id}.
 */
@Component
public class MessageSender {

    private static final Logger LOGGER = LoggerFactory.getLogger(MessageSender.class);

    private static final int TYPING_MAX_SECONDS = 5;
    private static final int TYPING_MIN_SECONDS = 1;
    private static final int CHARS_PER_SECOND = 20;

    /**
     * Отправка одного сообщения. Telegram-исключения логируются, не пробрасываются —
     * бизнес-поток не должен падать из-за неудачной доставки.
     */
    public void send(AbsSender bot, OutgoingMessage message) {
        sendTypingActionWithDuration(bot, message);
        SendMessage sm = toSendMessage(message);
        try {
            bot.execute(sm);
            LOGGER.info("✅ Sent | chat={} | thread={} | replyTo={} | text='{}'",
                    message.getChatId(),
                    message.getMessageThreadId(),
                    message.getReplyToMessageId(),
                    truncate(message.getText(), 100));
        } catch (TelegramApiException e) {
            LOGGER.error("❌ Send failed | chat={} | error={}", message.getChatId(), e.getMessage(), e);
        }
    }

    public void sendAll(AbsSender bot, Iterable<OutgoingMessage> messages) {
        if (messages == null) {
            return;
        }
        for (OutgoingMessage m : messages) {
            send(bot, m);
        }
    }

    private SendMessage toSendMessage(OutgoingMessage m) {
        SendMessage.SendMessageBuilder b = SendMessage.builder()
                .chatId(m.getChatId().toString())
                .text(m.getText());
        if (m.getMessageThreadId() != null) {
            b.messageThreadId(m.getMessageThreadId());
        }
        if (m.getReplyToMessageId() != null) {
            b.replyToMessageId(m.getReplyToMessageId());
        }
        if (m.getParseMode() != null) {
            b.parseMode(m.getParseMode());
        }
        if (m.getReplyMarkup() != null) {
            b.replyMarkup(m.getReplyMarkup());
        }
        if (m.isDisableWebPagePreview()) {
            b.disableWebPagePreview(true);
        }
        return b.build();
    }

    private void sendTypingActionWithDuration(AbsSender bot, OutgoingMessage m) {
        String text = m.getText();
        int duration = Math.max(TYPING_MIN_SECONDS,
                Math.min(TYPING_MAX_SECONDS, text != null ? text.length() / CHARS_PER_SECOND : TYPING_MIN_SECONDS));
        SendChatAction action = new SendChatAction();
        action.setChatId(m.getChatId().toString());
        action.setAction(ActionType.TYPING);
        if (m.getMessageThreadId() != null) {
            action.setMessageThreadId(m.getMessageThreadId());
        }
        try {
            for (int i = 0; i < duration; i++) {
                bot.execute(action);
                Thread.sleep(1000);
            }
            Thread.sleep(500);
        } catch (TelegramApiException e) {
            LOGGER.warn("⚠️ Typing action failed | chat={} | error={}", m.getChatId(), e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static String truncate(String text, int max) {
        if (text == null) {
            return null;
        }
        return text.length() > max ? text.substring(0, max) + "..." : text;
    }
}
