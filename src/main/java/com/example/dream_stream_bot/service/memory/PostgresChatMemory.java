package com.example.dream_stream_bot.service.memory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class PostgresChatMemory implements ChatMemory {
    private static final Logger logger = LoggerFactory.getLogger(PostgresChatMemory.class);

    /** Совпадает со схемой conversationId, которую формирует AbstractTelegramBot.buildConversationId. */
    private static final Pattern THREAD_ID_PATTERN = Pattern.compile(":thread:(\\d+)");

    @Autowired
    private PostgresChatMemoryRepository repository;

    @Override
    public void add(String conversationId, List<Message> messages) {
        if (messages == null || messages.isEmpty()) {
            return;
        }
        logger.info("💾 PostgresChatMemory.add() | ConversationId: {} | Messages count: {}",
                conversationId, messages.size());
        Integer threadId = extractThreadId(conversationId);
        TelegramMessageContext.MessageRef incomingRef = TelegramMessageContext.takeIncoming();
        int startIndex = repository.countByConversationId(conversationId);
        for (int i = 0; i < messages.size(); i++) {
            Message msg = messages.get(i);
            ChatMemoryEntity entity = new ChatMemoryEntity();
            entity.setConversationId(conversationId);
            entity.setMessageIndex(startIndex + i);
            entity.setRole(msg.getMessageType().name().toLowerCase());
            entity.setContent(extractText(msg));
            entity.setMessageThreadId(threadId);
            if (incomingRef != null && msg instanceof UserMessage) {
                entity.setTelegramMessageId(incomingRef.telegramMessageId());
            }
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
                    return switch (role) {
                        case "user" -> (Message) new UserMessage(e.getContent());
                        case "assistant" -> new AssistantMessage(e.getContent());
                        case "system" -> new SystemMessage(e.getContent());
                        default -> throw new IllegalArgumentException("Unknown role: " + e.getRole());
                    };
                })
                .collect(Collectors.toList());
    }

    @Override
    public void clear(String conversationId) {
        logger.info("💾 PostgresChatMemory.clear() | ConversationId: {}", conversationId);
        repository.deleteByConversationId(conversationId);
    }

    private static String extractText(Message msg) {
        if (msg instanceof UserMessage um) {
            return um.getText();
        }
        if (msg instanceof AssistantMessage am) {
            return am.getText();
        }
        if (msg instanceof SystemMessage sm) {
            return sm.getText();
        }
        return "";
    }

    private static Integer extractThreadId(String conversationId) {
        if (conversationId == null) {
            return null;
        }
        Matcher m = THREAD_ID_PATTERN.matcher(conversationId);
        if (m.find()) {
            try {
                return Integer.parseInt(m.group(1));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}
