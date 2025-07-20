package com.example.dream_stream_bot.service.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY;

@Service
public class AIServiceImpl implements AIService {

    private static final Logger logger = LoggerFactory.getLogger(AIServiceImpl.class);

    @Value("${bot.prompt}")
    private String systemPrompt;

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
    public String completion(long chatId, String message, String userName) {
        logger.info("🤖 AI Request | Chat: {} | User: {} | Message: '{}'", 
            chatId, userName, truncateText(message, 100));
        
        // Логируем конфигурацию для диагностики
        logger.info("🔧 OpenAI Config | API Key: {} | System Prompt: {}", 
            systemPrompt != null ? systemPrompt.substring(0, Math.min(50, systemPrompt.length())) + "..." : "null",
            systemPrompt != null ? "loaded" : "NOT LOADED");

        String response = chatClient.prompt()
                .advisors(a -> a
                        .param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, memoryWindowSize))
                .system(systemPrompt)
                .user(String.format("User %s says:\n%s", userName, message))
                .call()
                .content();

        logger.info("🤖 AI Response | Chat: {} | User: {} | Length: {} chars", 
            chatId, userName, response.length());
        logger.debug("🤖 AI Response content | Chat: {} | User: {} | Text: '{}'", 
            chatId, userName, truncateText(response, 200));

        return response;
    }

    @Override
    public String completion(long chatId, String message) {
        logger.info("🤖 AI Request | Chat: {} | Message: '{}'", 
            chatId, truncateText(message, 100));

        String response = chatClient.prompt()
                .advisors(a -> a
                        .param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, memoryWindowSize))
                .system(systemPrompt)
                .user(message)
                .call()
                .content();

        logger.info("🤖 AI Response | Chat: {} | Length: {} chars", 
            chatId, response.length());
        logger.debug("🤖 AI Response content | Chat: {} | Text: '{}'", 
            chatId, truncateText(response, 200));

        return response;
    }

    private String truncateText(String text, int maxLength) {
        if (text == null) return null;
        return text.length() > maxLength ? text.substring(0, maxLength) + "..." : text;
    }
}
