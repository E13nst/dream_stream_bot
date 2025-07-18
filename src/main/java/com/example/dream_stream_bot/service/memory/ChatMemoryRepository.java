package com.example.dream_stream_bot.service.memory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMemoryRepository extends JpaRepository<ChatMemoryEntity, Long> {
    List<ChatMemoryEntity> findByConversationIdOrderByMessageIndexAsc(String conversationId);
    int countByConversationId(String conversationId);
    void deleteByConversationId(String conversationId);
} 