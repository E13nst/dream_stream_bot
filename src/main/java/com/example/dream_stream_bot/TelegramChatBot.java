package com.example.dream_stream_bot;

import com.example.dream_stream_bot.config.BotConfig;
import com.example.dream_stream_bot.service.MessageHandlerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.ActionType;
import org.telegram.telegrambots.meta.api.methods.send.SendChatAction;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

@Component
@AllArgsConstructor
public class TelegramChatBot extends TelegramLongPollingBot {

    private static final Logger LOGGER = LoggerFactory.getLogger(TelegramChatBot.class);

    private static final ExecutorService executorService = Executors.newCachedThreadPool();

    private static final int CHARACTERS_PER_SECOND = 20;
    private static final int NOT_REPLY = 0;

    @Autowired
    private BotConfig botConfig;

    @Autowired
    private MessageHandlerService messageHandlerService;

    @Override
    public String getBotUsername() {
        return botConfig.getBotName();
    }

    @Override
    public String getBotToken() {
        return botConfig.getToken();
    }

    @Override
    public void onUpdateReceived(Update update) {

        if (update.hasMessage() && update.getMessage().hasText()) {

            String responseText = null;
            ObjectMapper objectMapper = new ObjectMapper();

            Message message = update.getMessage();
            Integer replyToMessageId = message.getMessageId();
            User user = message.getFrom();

            LOGGER.info(botConfig.getBotAliasesList().toString());

            LOGGER.info(message.toString());

            try {
                LOGGER.info("Message from {} [{}]: {}", user.getUserName(), user.getFirstName(), message.getText());
                LOGGER.info("Message from {} [{}]: {}", user.getUserName(), user.getFirstName(), objectMapper.writeValueAsString(message));

                // Персональное сообщение
                if (message.getChat().isUserChat()) {
                    replyToMessageId = NOT_REPLY;
                    responseText = messageHandlerService.handlePersonalMessage(message);
                }
                // Ответ на сообщение бота
                else if (message.isReply() && getBotUsername().equals(message.getReplyToMessage().getFrom().getUserName())) {
                    responseText = messageHandlerService.handleReplyToMessage(message);
                }
                // Упоминание имени бота
                else if (containsBotName(message.getText())) {
                    responseText = messageHandlerService.handleReplyToMessage(message);
                }
                // Сообщение в канале
                else if (message.getChat().isSuperGroupChat() && !message.getFrom().getIsBot()) {
                    responseText = messageHandlerService.handleSuperGroupMessage(message);
                }

                if (responseText != null) {
                    sendMessageWithTyping(message.getChatId(), user, responseText, replyToMessageId);
                }

            } catch (IOException e) {
                LOGGER.error("Error reading file: {}", e.getMessage());
                e.printStackTrace();
            }
        }

    }

    public boolean containsBotName(String text) {
        return Stream.concat(botConfig.getBotAliasesList().stream(), Stream.of(botConfig.getBotName()))
                .anyMatch(text::contains);
    }

    public String reply(Message message) {
        Message replyToMessage = message.getReplyToMessage();
        LOGGER.debug("REPLY TO MESSAGE: " + replyToMessage.getText());

        String repliedMessageUserName = replyToMessage.getFrom().getUserName();

        if (repliedMessageUserName != null && repliedMessageUserName.equals(getBotUsername())) {

            return String.format("%s писал: \"%s\"\n %s",
                    replyToMessage.getFrom().getFirstName(),
                    replyToMessage.getText(),
                    message.getText()
            );

//            return String.format("%s: \n> %s : \"%s\"\n\n%s",
//                    message.getFrom().getFirstName(),
//                    replyToMessage.getFrom().getFirstName(),
//                    replyToMessage.getText(),
//                    message.getText()
//            );

        } else {
            return message.getText();
        }
    }

    private void sendTypingAction(Long chatId, long durationInSeconds) {

        SendChatAction chatAction = new SendChatAction();
        chatAction.setChatId(chatId.toString());
        chatAction.setAction(ActionType.TYPING);

        try {
            long endTime = System.currentTimeMillis() + durationInSeconds * 1000;
            while (System.currentTimeMillis() < endTime) {
                execute(chatAction);
                Thread.sleep(5000);
            }
        } catch (TelegramApiException e) {
            LOGGER.error("Telegram API exception occurred", e);
        } catch (InterruptedException e) {
            LOGGER.error("Thread was interrupted", e);
            Thread.currentThread().interrupt();
        }
    }

    private void sendMessage(Long chatId, User user, String text, Integer replyToMessageId) {

        LOGGER.info("Response from {} [{}]: {}", user.getFirstName(), user.getUserName(), text);

        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        message.setReplyToMessageId(replyToMessageId);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            LOGGER.error("Failed to send message via Telegram API", e);
        }
    }

    private void sendMessageWithTyping(Long chatId, User user, String responseText, Integer replyToMessageId) {

        int durationInSeconds = responseText.length() / CHARACTERS_PER_SECOND;
        executorService.submit(() -> {
            sendTypingAction(chatId, durationInSeconds);
            sendMessage(chatId, user, responseText, replyToMessageId);
        });
    }

}
