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
        // В Spring AI 1.0.0 используем builder для создания PromptChatMemoryAdvisor
        PromptChatMemoryAdvisor advisor = PromptChatMemoryAdvisor.builder(postgresChatMemory).build();
        return chatClientBuilder
                .defaultAdvisors(advisor)
                .build();
    }
} 