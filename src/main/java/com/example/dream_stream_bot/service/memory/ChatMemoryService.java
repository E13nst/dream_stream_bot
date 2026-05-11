package com.example.dream_stream_bot.service.memory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Доменный сервис над {@link PostgresChatMemoryRepository} для команд /forget_last,
 * /forget_me и обработчика edited_message.
 */
@Service
public class ChatMemoryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChatMemoryService.class);

    private final PostgresChatMemoryRepository repository;

    public ChatMemoryService(PostgresChatMemoryRepository repository) {
        this.repository = repository;
    }

    /**
     * Применяет edited_message: ищет существующую запись по {@code (conversation_id, telegram_message_id)}
     * и заменяет в ней текст. Возвращает {@code true}, если запись была найдена.
     */
    @Transactional
    public boolean applyEdit(String conversationId, Integer telegramMessageId, String newContent) {
        if (conversationId == null || telegramMessageId == null) {
            return false;
        }
        Optional<ChatMemoryEntity> existing = repository.findByConversationIdAndTelegramMessageId(conversationId, telegramMessageId);
        if (existing.isEmpty()) {
            return false;
        }
        ChatMemoryEntity entity = existing.get();
        entity.setContent(newContent != null ? newContent : "");
        repository.save(entity);
        LOGGER.info("✏️ Edited memory entry | conv={} | tg_msg_id={} | new_len={}",
                conversationId, telegramMessageId, entity.getContent().length());
        return true;
    }

    /**
     * Удаляет последнюю пару user→assistant в разговоре.
     * Возвращает количество удалённых строк (0..2).
     */
    @Transactional
    public int forgetLast(String conversationId) {
        if (conversationId == null) {
            return 0;
        }
        List<ChatMemoryEntity> tail = repository.findTop2ByConversationIdOrderByMessageIndexDesc(conversationId);
        if (tail.isEmpty()) {
            return 0;
        }
        repository.deleteAll(tail);
        LOGGER.info("🗑 Forget last | conv={} | deleted={}", conversationId, tail.size());
        return tail.size();
    }

    /**
     * Удаляет все записи памяти пользователя в рамках конкретного бота.
     * Префикс: {@code bot:<botId>:...:user:<telegramUserId>}.
     */
    @Transactional
    public int forgetUser(Long botId, Long telegramUserId) {
        if (botId == null || telegramUserId == null) {
            return 0;
        }
        String botPrefix = "bot:" + botId;
        int removed = repository.deleteForUserInBot(botPrefix, String.valueOf(telegramUserId));
        LOGGER.info("🗑 Forget user | bot={} | tg_user={} | deleted={}", botId, telegramUserId, removed);
        return removed;
    }

    @Transactional
    public int deleteByConversationIdPrefix(String conversationIdPrefix) {
        if (conversationIdPrefix == null || conversationIdPrefix.isBlank()) {
            return 0;
        }
        return repository.deleteByConversationIdStartingWith(conversationIdPrefix);
    }

    @Transactional
    public int deleteConversationExact(String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            return 0;
        }
        return (int) repository.deleteAllByConversationId(conversationId);
    }
}
