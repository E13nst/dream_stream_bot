package com.example.dream_stream_bot.bot.error;

import com.example.dream_stream_bot.bot.message.MessageSender;
import com.example.dream_stream_bot.bot.message.OutgoingMessage;
import com.example.dream_stream_bot.exception.BotException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.bots.AbsSender;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;

/**
 * Централизованная обёртка вокруг {@code onUpdateReceived}.
 *
 * Лог + единое сообщение-заглушка в чат на случай {@link BotException} или
 * непойманного {@link RuntimeException}. Дедупликация по {@code chat_id} —
 * не более одного сообщения об ошибке в минуту в один чат.
 */
@Component
public class BotUpdateErrorHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(BotUpdateErrorHandler.class);
    private static final long DEDUP_WINDOW_MS = 60_000L;
    private static final String FALLBACK_MESSAGE = "⚠️ Извините, произошла ошибка. Попробуйте позже.";

    private final MessageSender messageSender;
    private final ConcurrentMap<Long, Long> lastErrorAtByChat = new ConcurrentHashMap<>();

    public BotUpdateErrorHandler(MessageSender messageSender) {
        this.messageSender = messageSender;
    }

    /**
     * Запустить обработчик update; ошибки логируются, в чат может отправиться
     * заглушка (с дедупликацией).
     */
    public void handle(AbsSender bot, String botUsername, Update update, Consumer<Update> action) {
        try {
            action.accept(update);
        } catch (BotException e) {
            LOGGER.warn("⚠️ BotException in {} update_id={}: {}", botUsername, update.getUpdateId(), e.getMessage());
            sendStubIfPossible(bot, update);
        } catch (RuntimeException e) {
            LOGGER.error("❌ Unhandled error in {} update_id={}: {}", botUsername, update.getUpdateId(), e.getMessage(), e);
            sendStubIfPossible(bot, update);
        }
    }

    private void sendStubIfPossible(AbsSender bot, Update update) {
        Message msg = update.getMessage() != null ? update.getMessage() : update.getEditedMessage();
        if (msg == null || msg.getChatId() == null) {
            return;
        }
        Long chatId = msg.getChatId();
        long now = System.currentTimeMillis();
        Long previous = lastErrorAtByChat.get(chatId);
        if (previous != null && now - previous < DEDUP_WINDOW_MS) {
            return;
        }
        lastErrorAtByChat.put(chatId, now);
        Integer threadId = Boolean.TRUE.equals(msg.getIsTopicMessage()) ? msg.getMessageThreadId() : null;
        messageSender.send(bot, OutgoingMessage.builder()
                .chatId(chatId)
                .messageThreadId(threadId)
                .text(FALLBACK_MESSAGE)
                .build());
    }
}
