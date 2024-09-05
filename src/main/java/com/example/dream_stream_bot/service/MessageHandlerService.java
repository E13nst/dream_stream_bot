package com.example.dream_stream_bot.service;

import com.example.dream_stream_bot.config.BotConfig;
import jakarta.annotation.PostConstruct;
import net.gcardone.junidecode.Junidecode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;

@Service
public class MessageHandlerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MessageHandlerService.class);

    @Autowired
    private BotConfig botConfig;
    @Autowired
    private AIService aiService;

    @PostConstruct
    public void init() {
    }

    public SendMessage handlePersonalMessage(Message message) {
        String response = aiService.completion(message.getFrom().getId(), message.getText(), chatUserName(message.getFrom()));
        return newTelegramMessage(message.getChatId(), response);
    }

    // Обработчик ответов на сообщения бота в чате
    public SendMessage handleReplyToBotMessage(Message message) {
        String response = aiService.completion(message.getChat().getId(), message.getText(), chatUserName(message.getFrom()));
        return newTelegramMessage(message.getChatId(), response, message.getMessageId());
    }

    // Обработчик сообщений канала
//    public SendMessage handleChannelMessage(Message message) {
//        String response = aiService.completion(message.getText(), message.getChat().getId(), chatChannelName(message.getFrom()));
//        return newTelegramMessage(message.getChatId(), response, message.getMessageId());
//    }

    public static String transliterateUserName(User user) {
        return Junidecode.unidecode(user.getFirstName()).replaceAll("[^a-zA-Z0-9_-]", "");
    }

    public static String chatUserName(User user) {
        return user.getLastName() == null ? user.getFirstName() : user.getFirstName() + " " + user.getLastName();
    }

    public SendMessage newTelegramMessage(long chatId, String text) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(text);
        sendMessage.enableMarkdown(true);
        return sendMessage;
    }

    public SendMessage newTelegramMessage(long chatId, String text, int replyToMessageId) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(text);
        sendMessage.setReplyToMessageId(replyToMessageId);
        sendMessage.enableMarkdown(true);
        return sendMessage;
    }
}
