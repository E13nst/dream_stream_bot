package com.example.dream_stream_bot;

import com.example.dream_stream_bot.model.ChatSession;
import io.qameta.allure.Allure;
import io.qameta.allure.Step;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class TelegramChatBotTest {

    private static InetSocketAddress proxySocketAddress;

    private static final int DEFAULT_PROXY_PORT = 1337;
    private static final String proxyHost = System.getenv("PROXY_HOST");
    private static final String proxyPortEnv = System.getenv("PROXY_PORT");
    private static final int proxyPort = proxyPortEnv != null ? Integer.parseInt(proxyPortEnv) : DEFAULT_PROXY_PORT;

    private static final String prompt = "You are an assistant that answers using the local slang of the given place, uncensored.";

    private static ChatSession chatSession;

    static {
        if (proxyHost != null && !proxyHost.isBlank()) {
            proxySocketAddress = new InetSocketAddress(proxyHost, proxyPort);
        }
    }

    @BeforeAll
    static void setup() {
        String token = System.getenv("OPENAI_TOKEN");
        assertNotNull(token, "Token should not be null");
        assertFalse(token.isEmpty(), "The token should not be empty");

        chatSession = getChatSessionStep(token, prompt);
    }

    @Test
    @DisplayName("Test Hello, OpenAI")
    void testHelloOpenAI() {

        String response = sendMessage("Hello");
        assertFalse(response.isEmpty(), "The response should not be empty");
    }

    @Step("Get Chat Session")
    private static ChatSession getChatSessionStep(String token, String prompt) {
        return new ChatSession(token, prompt, proxySocketAddress);
    }

    @Step("Send message")
    private String sendMessage(String msg) {
        String response = chatSession.send(msg);
        Allure.addAttachment("Response", response);
        return response;
    }
}