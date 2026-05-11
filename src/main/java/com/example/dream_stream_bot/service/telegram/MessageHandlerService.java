package com.example.dream_stream_bot.service.telegram;

import com.example.dream_stream_bot.bot.message.OutgoingMessage;
import com.example.dream_stream_bot.exception.AIServiceException;
import com.example.dream_stream_bot.model.agent.AgentConfigEntity;
import com.example.dream_stream_bot.model.telegram.BotEntity;
import com.example.dream_stream_bot.service.agent.AgentConfigService;
import com.example.dream_stream_bot.service.ai.AIService;
import com.example.dream_stream_bot.service.memory.TelegramMessageContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;

import java.util.ArrayList;
import java.util.List;

@Service
public class MessageHandlerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MessageHandlerService.class);

    @Autowired
    private AIService aiService;

    @Autowired
    private AgentConfigService agentConfigService;

    public List<OutgoingMessage> handleReplyToBotMessage(Message message, String conversationId, BotEntity botEntity) {
        User user = message.getFrom();
        LOGGER.info("💭 Handling reply to bot message | User: {} (@{}) | Text: '{}' | ChatId: {} | ConversationId: {}",
                user.getFirstName(), user.getUserName(), truncateText(message.getText(), 50), message.getChatId(), conversationId);
        String groupMessage = "User " + chatUserName(user) + " says:\n" + message.getText();
        AgentConfigEntity config = resolveAgentConfig(botEntity);
        String response;
        try {
            TelegramMessageContext.setIncoming(message.getMessageId(), threadIdOf(message));
            response = aiService.completion(conversationId, groupMessage, config);
        } finally {
            TelegramMessageContext.clear();
        }
        LOGGER.info("💬 Bot response to chatId {}: '{}'", message.getChatId(), truncateText(response, 100));

        List<OutgoingMessage> result = new ArrayList<>();
        result.add(OutgoingMessage.builder()
                .chatId(message.getChatId())
                .messageThreadId(threadIdOf(message))
                .replyToMessageId(message.getMessageId())
                .parseMode("Markdown")
                .text(response)
                .build());
        return result;
    }

    public List<OutgoingMessage> handlePersonalMessage(Message message, String conversationId, BotEntity botEntity) {
        User user = message.getFrom();
        LOGGER.info("💭 Handling personal message | User: {} (@{}) | Text: '{}' | ChatId: {} | ConversationId: {}",
                user.getFirstName(), user.getUserName(), truncateText(message.getText(), 50), message.getChatId(), conversationId);
        AgentConfigEntity base = resolveAgentConfig(botEntity);
        String system = base.getSystemPrompt() != null ? base.getSystemPrompt() : "";
        if (user.getFirstName() != null && !user.getFirstName().isEmpty()) {
            system = system + "\nUser name is " + user.getFirstName() + ".";
        }
        AgentConfigEntity config = copyWithSystemPrompt(base, system);
        String response;
        try {
            TelegramMessageContext.setIncoming(message.getMessageId(), threadIdOf(message));
            response = aiService.completion(conversationId, message.getText(), config);
        } finally {
            TelegramMessageContext.clear();
        }
        LOGGER.info("💬 Bot response to chatId {}: '{}'", message.getChatId(), truncateText(response, 100));

        List<OutgoingMessage> result = new ArrayList<>();
        result.add(OutgoingMessage.builder()
                .chatId(message.getChatId())
                .messageThreadId(threadIdOf(message))
                .parseMode("Markdown")
                .text(response)
                .build());
        return result;
    }

    private static Integer threadIdOf(Message message) {
        return Boolean.TRUE.equals(message.getIsTopicMessage()) ? message.getMessageThreadId() : null;
    }

    private AgentConfigEntity resolveAgentConfig(BotEntity botEntity) {
        if (botEntity.getAgentConfig() == null) {
            throw new AIServiceException("Бот не привязан к агенту (LLM). Выберите агента в настройках бота.");
        }
        Long id = botEntity.getAgentConfig().getId();
        AgentConfigEntity cached = agentConfigService.findById(id);
        if (cached == null) {
            throw new AIServiceException("Конфигурация агента не найдена (id=" + id + ").");
        }
        return cached;
    }

    private static AgentConfigEntity copyWithSystemPrompt(AgentConfigEntity src, String systemPrompt) {
        AgentConfigEntity copy = new AgentConfigEntity();
        BeanUtils.copyProperties(src, copy, "id", "createdAt", "updatedAt");
        copy.setSystemPrompt(systemPrompt);
        return copy;
    }

    public static String chatUserName(User user) {
        if (user.getFirstName() != null) {
            return user.getFirstName();
        } else if (user.getUserName() != null) {
            return user.getUserName();
        } else {
            return "User" + user.getId();
        }
    }

    private String truncateText(String text, int maxLength) {
        if (text == null) {
            return null;
        }
        return text.length() > maxLength ? text.substring(0, maxLength) + "..." : text;
    }
}
