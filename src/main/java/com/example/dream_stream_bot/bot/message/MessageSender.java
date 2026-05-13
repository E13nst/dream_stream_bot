package com.example.dream_stream_bot.bot.message;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.ActionType;
import org.telegram.telegrambots.meta.api.methods.send.SendChatAction;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Единая точка отправки сообщений в Telegram. Проксирует {@link OutgoingMessage}
 * через {@link AbsSender} с правильным проставлением {@code message_thread_id}
 * (форум-топики) и {@code reply_to_message_id}.
 */
@Component
public class MessageSender {

    private static final Logger LOGGER = LoggerFactory.getLogger(MessageSender.class);

    /** Ручка keep-alive «печатает»: {@link #close()} не бросает checked-исключения. */
    @FunctionalInterface
    public interface TypingKeepAliveHandle extends AutoCloseable {
        @Override
        void close();
    }

    /** Telegram держит статус «печатает» около 5 секунд — обновляем чуть раньше. */
    private static final int TYPING_REFRESH_SECONDS = 4;

    /**
     * Периодически отправляет {@code typing}, пока не вызван {@link TypingKeepAliveHandle#close()} —
     * для ожидания ответа LLM. Команды и меню должны отправляться через {@link #send}
     * без этой сессии.
     */
    public TypingKeepAliveHandle startTypingKeepAlive(AbsSender bot, Long chatId, Integer messageThreadId) {
        if (chatId == null) {
            return () -> { };
        }
        sendTyping(bot, chatId, messageThreadId);
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "telegram-typing-keepalive");
            t.setDaemon(true);
            return t;
        });
        Runnable refresh = () -> {
            try {
                sendTyping(bot, chatId, messageThreadId);
            } catch (Exception e) {
                LOGGER.warn("⚠️ Typing refresh failed | chat={} | error={}", chatId, e.getMessage());
            }
        };
        scheduler.scheduleAtFixedRate(refresh, TYPING_REFRESH_SECONDS, TYPING_REFRESH_SECONDS, TimeUnit.SECONDS);
        return () -> {
            scheduler.shutdownNow();
            try {
                scheduler.awaitTermination(2, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        };
    }

    private void sendTyping(AbsSender bot, Long chatId, Integer messageThreadId) {
        SendChatAction action = new SendChatAction();
        action.setChatId(chatId.toString());
        action.setAction(ActionType.TYPING);
        if (messageThreadId != null) {
            action.setMessageThreadId(messageThreadId);
        }
        try {
            bot.execute(action);
        } catch (TelegramApiException e) {
            LOGGER.warn("⚠️ Typing action failed | chat={} | error={}", chatId, e.getMessage());
        }
    }

    /**
     * Отправка одного сообщения. Telegram-исключения логируются, не пробрасываются —
     * бизнес-поток не должен падать из-за неудачной доставки.
     */
    public void send(AbsSender bot, OutgoingMessage message) {
        trySend(bot, message);
    }

    /**
     * Как {@link #send}, но возвращает признак успеха (для UX при доставке в группу по кнопке).
     */
    public boolean trySend(AbsSender bot, OutgoingMessage message) {
        SendMessage sm = toSendMessage(message);
        try {
            bot.execute(sm);
            LOGGER.info("✅ Sent | chat={} | thread={} | replyTo={} | text='{}'",
                    message.getChatId(),
                    message.getMessageThreadId(),
                    message.getReplyToMessageId(),
                    truncate(message.getText(), 100));
            return true;
        } catch (TelegramApiException e) {
            LOGGER.error("❌ Send failed | chat={} | error={}", message.getChatId(), e.getMessage(), e);
            return false;
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

    private static String truncate(String text, int max) {
        if (text == null) {
            return null;
        }
        return text.length() > max ? text.substring(0, max) + "..." : text;
    }
}
