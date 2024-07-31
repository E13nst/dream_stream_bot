package com.example.dream_stream_bot;

import com.example.dream_stream_bot.model.ChatSession;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;

import static org.junit.jupiter.api.Assertions.assertFalse;

class TelegramChatBotTest {

    private static InetSocketAddress proxySocketAddress;

    private static final int DEFAULT_PROXY_PORT = 1337;
    private static final String proxyHost = System.getenv("PROXY_HOST");
    private static final String proxyPortEnv = System.getenv("PROXY_PORT");
    private static final int proxyPort = proxyPortEnv != null ? Integer.parseInt(proxyPortEnv) : DEFAULT_PROXY_PORT;

//    private static final String token = System.getenv("OPENAI_TOKEN");

    static {
        if (proxyHost != null && !proxyHost.isBlank()) {
            proxySocketAddress = new InetSocketAddress(proxyHost, proxyPort);
        }
    }

    @BeforeAll
    @Disabled
    static void setup() {
//        String token = System.getenv("OPENAI_TOKEN");
//        assertFalse(token.isEmpty(), "The token should not be empty");
    }

    @Test
    @Disabled
    void handlePersonalMessage() {

        String token = System.getenv("OPENAI_TOKEN");
        assertFalse(token.isEmpty(), "The token should not be empty");

        ChatSession chatSession = new ChatSession(token, "You are an assistant that answers using the local slang of the given place, uncensored.", proxySocketAddress);
        String response = chatSession.send("Hello");

        assertFalse(response.isEmpty(), "The response should not be empty");
    }
}