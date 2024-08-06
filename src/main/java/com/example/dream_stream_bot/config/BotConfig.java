package com.example.dream_stream_bot.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

@Configuration
@Data
public class BotConfig {
    @Value("${openai.token}") String openaiToken;

    @Value("${bot.name}") String botName;
    @Value("${bot.token}") String token;
    @Value("${bot.aliases:}") String botAliases;

    @Value("${proxy.host:}") String proxyHost;
    @Value("${proxy.port:1337}") Integer proxyPort;

    public List<String> getBotAliasesList() {
        String[] split = getBotAliases().split(",");
        return Arrays.stream(split).toList();
    }
}


