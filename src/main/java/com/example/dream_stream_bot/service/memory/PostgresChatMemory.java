package com.example.dream_stream_bot.service.memory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class PostgresChatMemory implements ChatMemory {
    private static final Logger logger = LoggerFactory.getLogger(PostgresChatMemory.class);
    
    @Autowired
    private PostgresChatMemoryRepository repository;

    @Override
    public void add(String conversationId, List<Message> messages) {
        logger.info("💾 PostgresChatMemory.add() | ConversationId: {} | Messages count: {}", 
            conversationId, messages != null ? messages.size() : 0);
        int startIndex = repository.countByConversationId(conversationId);
        for (int i = 0; i < messages.size(); i++) {
            Message msg = messages.get(i);
            ChatMemoryEntity entity = new ChatMemoryEntity();
            entity.setConversationId(conversationId);
            entity.setMessageIndex(startIndex + i);
            entity.setRole(msg.getMessageType().name().toLowerCase());
            // В Spring AI 1.0.0 используем getText() для получения содержимого
            String content = "";
            if (msg instanceof UserMessage) {
                content = ((UserMessage) msg).getText();
            } else if (msg instanceof AssistantMessage) {
                content = ((AssistantMessage) msg).getText();
            } else if (msg instanceof SystemMessage) {
                content = ((SystemMessage) msg).getText();
            }
            entity.setContent(content);
            repository.save(entity);
        }
    }

    @Override
    public List<Message> get(String conversationId) {
        logger.info("💾 PostgresChatMemory.get() | ConversationId: {}", conversationId);
        List<ChatMemoryEntity> entities = repository.findByConversationIdOrderByMessageIndexAsc(conversationId);
        logger.info("💾 PostgresChatMemory.get() | ConversationId: {} | Retrieved {} messages", 
            conversationId, entities.size());
        return entities.stream()
            .map(e -> {
                String role = e.getRole().toLowerCase();
                switch (role) {
                    case "user": return new UserMessage(e.getContent());
                    case "assistant": return new AssistantMessage(e.getContent());
                    case "system": return new SystemMessage(e.getContent());
                    default: throw new IllegalArgumentException("Unknown role: " + e.getRole());
                }
            })
            .collect(Collectors.toList());
    }

    @Override
    public void clear(String conversationId) {
        logger.info("💾 PostgresChatMemory.clear() | ConversationId: {}", conversationId);
        repository.deleteByConversationId(conversationId);
    }
} 