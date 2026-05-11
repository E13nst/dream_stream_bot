package com.example.dream_stream_bot.service.memory;

/**
 * ThreadLocal-контекст для проброса {@code telegram_message_id} от точки приёма
 * Telegram-апдейта внутрь {@link PostgresChatMemory#add(String, java.util.List)},
 * куда Spring AI вызывает сохранение без передачи дополнительного контекста.
 *
 * Контекст должен быть установлен непосредственно перед вызовом {@code aiService.completion(...)}
 * и очищается автоматически при первом чтении (см. {@link #takeIncoming()}).
 */
public final class TelegramMessageContext {

    private static final ThreadLocal<MessageRef> INCOMING = new ThreadLocal<>();

    private TelegramMessageContext() {
    }

    public static void setIncoming(Integer telegramMessageId, Integer messageThreadId) {
        if (telegramMessageId == null && messageThreadId == null) {
            INCOMING.remove();
            return;
        }
        INCOMING.set(new MessageRef(telegramMessageId, messageThreadId));
    }

    /** Возвращает контекст и очищает ThreadLocal. */
    static MessageRef takeIncoming() {
        MessageRef ref = INCOMING.get();
        INCOMING.remove();
        return ref;
    }

    public static void clear() {
        INCOMING.remove();
    }

    public record MessageRef(Integer telegramMessageId, Integer messageThreadId) {
    }
}
