package com.example.dream_stream_bot.service.telegram;

import com.example.dream_stream_bot.config.BotConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;

import java.util.ArrayList;
import java.util.List;

@Service
public class MessageHandlerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MessageHandlerService.class);

    @Autowired
    private BotConfig botConfig;
    @Autowired
    private com.example.dream_stream_bot.service.ai.AIService aiService;

    // Обработчик ответов на сообщения бота в чате
    public List<SendMessage> handleReplyToBotMessage(Message message) {
        User user = message.getFrom();
        LOGGER.info("\uD83D\uDCAC Handling reply to bot message | User: {} (@{}) | Text: '{}'", 
            user.getFirstName(), user.getUserName(), truncateText(message.getText(), 50));

        List<SendMessage> sendMessages = new ArrayList<>();
        TelegramMessageFactory msgFactory = new TelegramMessageFactory(message.getChatId());
        String response = aiService.completion(message.getChat().getId(), message.getText(), chatUserName(message.getFrom()));
        sendMessages.add(msgFactory.createReplyToMessage(response, message.getMessageId()));
        LOGGER.info("\uD83D\uDCAC Reply message prepared | User: {} (@{}) | Response length: {} chars", 
            user.getFirstName(), user.getUserName(), response.length());
        return sendMessages;
    }

    public List<SendMessage> handlePersonalMessage(Message message) {
        User user = message.getFrom();
        LOGGER.info("\uD83D\uDCAD Handling personal message | User: {} (@{}) | Text: '{}'", 
            user.getFirstName(), user.getUserName(), truncateText(message.getText(), 50));

        List<SendMessage> sendMessages = new ArrayList<>();
        TelegramMessageFactory msgFactory = new TelegramMessageFactory(message.getChatId());
        String response = aiService.completion(user.getId(), message.getText(), chatUserName(user));
        sendMessages.add(msgFactory.createMarkdownMessage(response));
        return sendMessages;
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

    public static String transliterateUserName(User user) {
        return user.getFirstName();
    }

    private String truncateText(String text, int maxLength) {
        if (text == null) return null;
        return text.length() > maxLength ? text.substring(0, maxLength) + "..." : text;
    }
}
