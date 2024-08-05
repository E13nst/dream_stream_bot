package com.example.dream_stream_bot.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.net.InetSocketAddress;

@Configuration
@Data
@PropertySource("application.properties")
public class BotConfig {
    @Value("${openai.token}") String openaiToken;

    @Value("${bot.name}") String botName;
    @Value("${bot.token}") String token;

    @Value("${proxy.host:}") String proxyHost;
    @Value("${proxy.port:1337}") Integer proxyPort;

    public InetSocketAddress getProxySocketAddress() {
        if (proxyHost == null || proxyHost.trim().isEmpty()) {
            return null;
        }

        int port = (proxyPort != null) ? proxyPort : 1337;

        return new InetSocketAddress(proxyHost, port);
    }
}


