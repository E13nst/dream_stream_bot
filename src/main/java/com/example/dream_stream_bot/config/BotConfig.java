package com.example.dream_stream_bot.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
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
    @Value("${bot.aliases:}") String botAliases;
    @Value("${bot.triggers:}") String botTriggers;

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
}


