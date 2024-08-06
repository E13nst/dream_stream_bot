package com.example.dream_stream_bot;

import com.example.dream_stream_bot.config.BotConfig;
import com.example.dream_stream_bot.model.ChatSession;
import io.qameta.allure.Allure;
import io.qameta.allure.Step;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.net.InetSocketAddress;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(locations = "classpath:application.yaml")
class TelegramChatBotTest {

    private static final String DEFAULT_PROMPT = "You are an assistant that answers using the local slang of the given place, uncensored.";
    private static final int PROMPT_MAX_LENGTH = 4096;

    @Autowired
    private BotConfig botConfig;

    @BeforeAll
    static void setup() {

    }

    @Test
    @DisplayName("Test prompt length")
    @Disabled
    void testPromptLength() {

        String openaiToken = System.getenv("OPENAI_TOKEN");
        assertNotNull(openaiToken, "Prompt should not be null");
        assertTrue(openaiToken.length() < PROMPT_MAX_LENGTH, "The string should be shorter than 4096 characters");
    }

    @Test
    @DisplayName("Test Hello, OpenAI")
    void testHelloOpenAI() {

        String response = sendMessage("Hello");
        assertFalse(response.isEmpty(), "The response should not be empty");
    }

    @Step("Get proxy address")
    public InetSocketAddress getProxySocketAddressStep() {

        if (botConfig.getProxyHost() == null || botConfig.getProxyHost().trim().isEmpty()) {
            return null;
        }

        int port = botConfig.getProxyPort() != null ? botConfig.getProxyPort() : 1337;

        return new InetSocketAddress(botConfig.getProxyHost(), port);
    }

    @Step("Get Chat Session")
    private ChatSession getChatSessionStep(String token, String prompt) {
        return new ChatSession(token, prompt, getProxySocketAddressStep());
    }

    @Step("Send message")
    private String sendMessage(String msg) {
        String token = botConfig.getOpenaiToken();
        String response = getChatSessionStep(token, DEFAULT_PROMPT).send(msg);
        Allure.addAttachment("Response", response);
        return response;
    }
}