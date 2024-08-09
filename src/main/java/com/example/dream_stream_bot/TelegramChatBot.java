package com.example.dream_stream_bot;

import com.example.dream_stream_bot.config.BotConfig;
import com.example.dream_stream_bot.model.Commands;
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
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

@Component
@AllArgsConstructor
public class TelegramChatBot extends TelegramLongPollingBot {

    private static final Logger LOGGER = LoggerFactory.getLogger(TelegramChatBot.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final int CHARACTERS_PER_SECOND = 20;

    private static final ExecutorService executorService = Executors.newCachedThreadPool();

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

            SendMessage response = null;

            Message message = update.getMessage();
            User user = message.getFrom();

            LOGGER.info(message.toString());

            try {
                LOGGER.info("Message from {} [{}]: {}", user.getUserName(), user.getFirstName(), message.getText());
                LOGGER.info("Message from {} [{}]: {}", user.getUserName(), user.getFirstName(), objectMapper.writeValueAsString(message));

                // Обработка команд
                if (isCommand(message)) {
//                    responseText = switch (message.getText()) {
//                        case "/start" -> messageHandlerService.startCommandReceived(message.getChatId(), user);
//                        case "/help" -> messageHandlerService.helpCommandReceived(message.getChatId(), user);
//                        default -> messageHandlerService.handlePersonalMessage(message);
//                    };
                }

                // Персональное сообщение
                else if (message.getChat().isUserChat()) {
                    response = messageHandlerService.handlePersonalMessage(message);
                }
                // Сообщение в группе
                else if (message.getChat().isGroupChat()) {
                    // Ответ на сообщение бота
                    if (message.isReply() && getBotUsername().equals(message.getReplyToMessage().getFrom().getUserName())) {
                        response = messageHandlerService.handleReplyToBotMessage(message);
                    }
                    // Упоминание имени бота
                    else if (containsBotName(message.getText()) || containsTriggers(message.getText())) {
                        response = messageHandlerService.handleReplyToBotMessage(message);
                    }
                }
                // Сообщение в канале
                else if (message.getChat().isSuperGroupChat() && !message.getFrom().getIsBot()) {
                    response = messageHandlerService.handleSuperGroupMessage(message);
                }

                if (response != null) {
                    sendMessageWithTyping(message.getChatId(), user, response);
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

    public boolean containsTriggers(String text) {
        return botConfig.getBotTriggersList().stream().anyMatch(text.toLowerCase(Locale.ROOT)::contains);
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

    private void sendMessage(User user, SendMessage message) {

        LOGGER.info("Response from {} [{}]: {}", user.getFirstName(), user.getUserName(), message.getText());

        try {
            execute(message);
        } catch (TelegramApiException e) {
            LOGGER.error("Failed to send message via Telegram API", e);
        }
    }

    private void sendMessageWithTyping(Long chatId, User user, SendMessage message) {

        int durationInSeconds = message.getText().length() / CHARACTERS_PER_SECOND;
        executorService.submit(() -> {
            sendTypingAction(chatId, durationInSeconds);
            sendMessage(user, message);
        });
    }

    boolean isCommand(Message message) {
        return  message.getChat().isUserChat() &&
                Arrays.stream(Commands.values()).map(Commands::toString)
                    .anyMatch(message.getText()::contains);
    }

}
