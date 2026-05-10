package com.example.dream_stream_bot.service.ai;

import com.example.dream_stream_bot.model.agent.AgentConfigEntity;
import com.example.dream_stream_bot.model.agent.AgentProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AIServiceImpl implements AIService {

    private static final Logger logger = LoggerFactory.getLogger(AIServiceImpl.class);

    private static final String CHAT_MEMORY_CONVERSATION_ID_KEY = "conversation-id";
    private static final String CHAT_MEMORY_RETRIEVE_SIZE_KEY = "chat-memory-retrieve-size";

    private final ChatClient chatClient;
    private final ChatMemory chatMemory;

    @Autowired
    public AIServiceImpl(ChatClient chatClient, ChatMemory chatMemory) {
        this.chatClient = chatClient;
        this.chatMemory = chatMemory;
    }

    @Override
    public String completion(String conversationId, String userMessage, AgentConfigEntity agentConfig) {
        if (agentConfig.getProvider() != AgentProvider.OPENAI) {
            throw new UnsupportedOperationException(
                    "Provider " + agentConfig.getProvider() + " is not wired yet; only OPENAI is supported.");
        }

        logger.info("\uD83E\uDD16 AI Request | Conversation: {} | Model: {} | Message: '{}'",
                conversationId, agentConfig.getModel(), truncateText(userMessage, 100));
        logger.debug("\uD83E\uDD16 AI Request | Using conversation_id key: '{}' | Value: '{}'",
                CHAT_MEMORY_CONVERSATION_ID_KEY, conversationId);

        PromptChatMemoryAdvisor advisor = PromptChatMemoryAdvisor.builder(chatMemory)
                .conversationId(conversationId)
                .build();

        int retrieveSize = agentConfig.getMemWindow() != null ? agentConfig.getMemWindow() : 100;
        OpenAiChatOptions.Builder optionsBuilder = OpenAiChatOptions.builder()
                .model(agentConfig.getModel());
        applySamplingOptions(optionsBuilder, agentConfig);

        String response;
        try {
            response = callModel(conversationId, userMessage, agentConfig, advisor, retrieveSize, optionsBuilder.build());
        } catch (RuntimeException e) {
            if (!isUnsupportedSamplingOption(e)) {
                throw e;
            }
            logger.warn("\uD83E\uDD16 AI Request | Model '{}' rejected custom sampling options; retrying with explicit OpenAI defaults",
                    agentConfig.getModel(), e);
            response = callModel(
                    conversationId,
                    userMessage,
                    agentConfig,
                    advisor,
                    retrieveSize,
                    defaultSamplingOptions(agentConfig.getModel()));
        }
        logger.info("\uD83E\uDD16 AI Response | Conversation: {} | Length: {} chars",
                conversationId, response.length());
        logger.debug("\uD83E\uDD16 AI Response content | Conversation: {} | Text: '{}'",
                conversationId, truncateText(response, 200));
        return response;
    }

    private String truncateText(String text, int maxLength) {
        if (text == null) {
            return null;
        }
        return text.length() > maxLength ? text.substring(0, maxLength) + "..." : text;
    }

    private void applySamplingOptions(OpenAiChatOptions.Builder optionsBuilder, AgentConfigEntity agentConfig) {
        if (agentConfig.getTemperature() != null) {
            optionsBuilder.temperature(agentConfig.getTemperature());
        }
        if (agentConfig.getTopP() != null) {
            optionsBuilder.topP(agentConfig.getTopP());
        }
        if (agentConfig.getFrequencyPenalty() != null) {
            optionsBuilder.frequencyPenalty(agentConfig.getFrequencyPenalty());
        }
        if (agentConfig.getPresencePenalty() != null) {
            optionsBuilder.presencePenalty(agentConfig.getPresencePenalty());
        }
    }

    private String callModel(String conversationId,
                             String userMessage,
                             AgentConfigEntity agentConfig,
                             PromptChatMemoryAdvisor advisor,
                             int retrieveSize,
                             OpenAiChatOptions options) {
        return chatClient.prompt()
                .options(options)
                .advisors(advisor)
                .advisors(a -> a.param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, retrieveSize))
                .system(agentConfig.getSystemPrompt() != null ? agentConfig.getSystemPrompt() : "")
                .user(userMessage)
                .call()
                .content();
    }

    private boolean isUnsupportedSamplingOption(RuntimeException e) {
        Throwable current = e;
        while (current != null) {
            String message = current.getMessage();
            if (message != null && isUnsupportedSamplingMessage(message)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private boolean isUnsupportedSamplingMessage(String message) {
        String normalized = message.toLowerCase();
        return (normalized.contains("unsupported_value") || normalized.contains("unsupported value"))
                && (normalized.contains("temperature")
                || normalized.contains("top_p")
                || normalized.contains("frequency_penalty")
                || normalized.contains("presence_penalty"));
    }

    private OpenAiChatOptions defaultSamplingOptions(String model) {
        return OpenAiChatOptions.builder()
                .model(model)
                .temperature(1.0)
                .topP(1.0)
                .frequencyPenalty(0.0)
                .presencePenalty(0.0)
                .build();
    }
}
