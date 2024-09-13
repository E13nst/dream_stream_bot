package com.example.dream_stream_bot.config;

import com.example.dream_stream_bot.service.InMemoryChatMemory;
import lombok.Data;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;

@Configuration
@Data
public class BotConfig {
    @Value("${openai.token}") String openaiToken;

    @Value("${bot.name}") String botName;
    @Value("${bot.token}") String token;
    @Value("${bot.prompt}") String prompt;
    @Value("${bot.aliases:}") String botAliases;
    @Value("${bot.triggers:}") String botTriggers;
    @Value("${bot.description.start}") String startDescription;

    @Value("${proxy.host:}") String proxyHost;
    @Value("${proxy.port:1337}") Integer proxyPort;

    public List<String> getBotAliasesList() {
        String[] split = getBotAliases().split(",");
        return Arrays.stream(split).toList();
    }
    public List<String> getBotTriggersList() {
        String[] split = getBotTriggers().split(",");
        return Arrays.stream(split).toList();
    }

    public InetSocketAddress getProxySocketAddress() {

        if (proxyHost == null || proxyHost.trim().isEmpty()) {
            return null;
        }

        int port = proxyPort != null ? proxyPort : 1337;
        return new InetSocketAddress(proxyHost, port);
    }

    @Bean
    public InMemoryChatMemory inMemoryChatMemory() {
        return new InMemoryChatMemory();
    }

    @Bean
    public ChatClient chatClient(ChatClient.Builder chatClientBuilder, InMemoryChatMemory inMemoryChatMemory, BotConfig botConfig) {

        return chatClientBuilder
                .defaultAdvisors(new PromptChatMemoryAdvisor(inMemoryChatMemory))
                .defaultSystem(botConfig.getPrompt())
                .build();
    }

}


