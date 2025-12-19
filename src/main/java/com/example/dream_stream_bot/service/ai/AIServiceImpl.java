package com.example.dream_stream_bot.service.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AIServiceImpl implements AIService {

    private static final Logger logger = LoggerFactory.getLogger(AIServiceImpl.class);
    
    // Константы для ChatMemory Advisor (Spring AI 1.0.0)
    // В Spring AI 1.0.0 используется "conversation-id" как ключ параметра для PromptChatMemoryAdvisor
    // Это правильное имя параметра согласно документации Spring AI
    private static final String CHAT_MEMORY_CONVERSATION_ID_KEY = "conversation-id";
    private static final String CHAT_MEMORY_RETRIEVE_SIZE_KEY = "chat-memory-retrieve-size";

    @Value("${bot.memory-window-size:100}")
    private int memoryWindowSize;

    private final ChatClient chatClient;
    private final ChatMemory chatMemory;

    @Autowired
    public AIServiceImpl(ChatClient chatClient, ChatMemory chatMemory) {
        this.chatClient = chatClient;
        this.chatMemory = chatMemory;
    }

    @Override
    public String completion(String conversationId, String message, String prompt, Integer memWindow) {
        logger.info("\uD83E\uDD16 AI Request | Conversation: {} | Message: '{}'", 
            conversationId, truncateText(message, 100));
        logger.debug("\uD83E\uDD16 AI Request | Using conversation_id key: '{}' | Value: '{}'", 
            CHAT_MEMORY_CONVERSATION_ID_KEY, conversationId);
        String response = chatClient.prompt()
                .advisors(a -> {
                    a.param(CHAT_MEMORY_CONVERSATION_ID_KEY, conversationId);
                    a.param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, memWindow != null ? memWindow : 100);
                    logger.debug("\uD83E\uDD16 AI Request | Advisor params set | conversation_id: '{}' | retrieve_size: {}", 
                        conversationId, memWindow != null ? memWindow : 100);
                })
                .system(prompt)
                .user(message)
                .call()
                .content();
        logger.info("\uD83E\uDD16 AI Response | Conversation: {} | Length: {} chars", 
            conversationId, response.length());
        logger.debug("\uD83E\uDD16 AI Response content | Conversation: {} | Text: '{}'", 
            conversationId, truncateText(response, 200));
        return response;
    }

    private String truncateText(String text, int maxLength) {
        if (text == null) return null;
        return text.length() > maxLength ? text.substring(0, maxLength) + "..." : text;
    }
}
