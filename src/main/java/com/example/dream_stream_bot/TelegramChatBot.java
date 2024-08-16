package com.example.dream_stream_bot;

import com.example.dream_stream_bot.config.BotConfig;
import com.example.dream_stream_bot.model.InlineButtons;
import com.example.dream_stream_bot.service.CommandHandlerService;
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
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

@Component
@AllArgsConstructor
public class TelegramChatBot extends TelegramLongPollingBot {

    private static final Logger LOGGER = LoggerFactory.getLogger(TelegramChatBot.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final ExecutorService executorService = Executors.newCachedThreadPool();

    private static final int CHARACTERS_PER_SECOND = 20;

    @Autowired
    private BotConfig botConfig;

    @Autowired
    private MessageHandlerService messageHandlerService;

    @Autowired
    private CommandHandlerService commandHandlerService;

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

                // Персональное сообщение
                if (message.getChat().isUserChat()) {
                    response = switch (message.getText()) {
                        case "/start" -> commandHandlerService.start(message);
                        case "/help" -> commandHandlerService.help(message.getChatId());
                        // Персональное сообщение
                        default -> commandHandlerService.handlePersonalMessage(message);
//                        default -> messageHandlerService.handlePersonalMessage(message);
                    };
                }
                // Сообщение в группе
                else if (message.getChat().isGroupChat() || message.getChat().isSuperGroupChat()) {
                    // Ответ на сообщение бота
                    if (message.isReply() && getBotUsername().equals(message.getReplyToMessage().getFrom().getUserName())) {
                        response = messageHandlerService.handleReplyToBotMessage(message);
                    }
                    // Упоминание имени бота
                    else if (containsBotName(message.getText()) || containsTriggers(message.getText())) {
                        response = messageHandlerService.handleReplyToBotMessage(message);
                    }
                }

                if (response != null && !response.getText().isBlank()) {
                    LOGGER.info("Response from {} [{}]: {}", user.getFirstName(), user.getUserName(), response);
                    sendMessageWithTyping(message.getChatId(), response);
                }

            } catch (IOException e) {
                LOGGER.error("Error: {}", e.getMessage());
                e.printStackTrace();
            }
        }
        else if (update.hasCallbackQuery()) {

            var callbackQuery = update.getCallbackQuery();
            long chatId = callbackQuery.getMessage().getChatId();
            var user = callbackQuery.getFrom();

            LOGGER.info("CallbackQuery {} [{}]: {}", user.getFirstName(), user.getUserName(), callbackQuery);

            InlineButtons button = Optional.ofNullable(callbackQuery.getData())
                    .map(data -> {
                        try {
                            return InlineButtons.valueOf(data);
                        } catch (IllegalArgumentException e) {
                            return null;
                        }
                    })
                    .orElse(InlineButtons.NONE);

            SendMessage response = switch (button) {
                case PREVIOUS -> commandHandlerService.previous(callbackQuery);
                case NEXT -> commandHandlerService.next(callbackQuery);
                case CANCEL -> commandHandlerService.delete(callbackQuery);
                default -> commandHandlerService.help(chatId);
            };

            if (response != null) {
                LOGGER.info("Response from {} [{}]: {}", user.getFirstName(), user.getUserName(), response.getText());
                sendMessage(response);
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

    private void sendMessage(SendMessage message) {

        try {
            execute(message);
        } catch (TelegramApiException e) {
            LOGGER.error("Failed to send message via Telegram API", e);
        }
    }

    private void sendMessageWithTyping(Long chatId, SendMessage message) {

        int durationInSeconds = message.getText().length() / CHARACTERS_PER_SECOND;
        executorService.submit(() -> {
            sendTypingAction(chatId, durationInSeconds);
            sendMessage(message);
        });
    }

}
