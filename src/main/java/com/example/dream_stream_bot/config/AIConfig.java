package com.example.dream_stream_bot.config;

import com.example.dream_stream_bot.service.memory.PostgresChatMemory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AIConfig {
    @Bean
    public PostgresChatMemory postgresChatMemory() {
        return new PostgresChatMemory();
    }

    @Bean
    public ChatClient chatClient(ChatClient.Builder chatClientBuilder, PostgresChatMemory postgresChatMemory) {
        // Не используем defaultAdvisors, так как advisor создается для каждого запроса с правильным conversation_id
        // Это позволяет правильно разделять контексты разных ботов и чатов
        return chatClientBuilder
                .build();
    }
} 