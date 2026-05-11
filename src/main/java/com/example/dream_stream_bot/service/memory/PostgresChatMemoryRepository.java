package com.example.dream_stream_bot.service.memory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PostgresChatMemoryRepository extends JpaRepository<ChatMemoryEntity, Long> {
    List<ChatMemoryEntity> findByConversationIdOrderByMessageIndexAsc(String conversationId);

    int countByConversationId(String conversationId);

    void deleteByConversationId(String conversationId);

    Optional<ChatMemoryEntity> findByConversationIdAndTelegramMessageId(String conversationId, Integer telegramMessageId);

    List<ChatMemoryEntity> findTop2ByConversationIdOrderByMessageIndexDesc(String conversationId);

    /** Все сообщения в рамках группового чата (все топики и пользователи): {@code bot:<id>:chat:<chatId>}% */
    @Modifying
    @Query("delete from ChatMemoryEntity c where c.conversationId like concat(:prefix, '%')")
    int deleteByConversationIdStartingWith(@Param("prefix") String prefix);

    /**
     * Удаляет все записи памяти пользователя Telegram в рамках бота (личка + все группы этого бота).
     * Суффикс разговора: {@code :user:<telegram_id>}.
     */
    @Modifying
    @Query("delete from ChatMemoryEntity c where c.conversationId like concat(:botPrefix, '%') and c.conversationId like concat('%', ':user:', :telegramUserId)")
    int deleteForUserInBot(@Param("botPrefix") String botPrefix, @Param("telegramUserId") String telegramUserId);

    @Modifying
    long deleteAllByConversationId(String conversationId);
}
