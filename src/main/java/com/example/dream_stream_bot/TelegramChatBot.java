package com.example.dream_stream_bot;

import com.example.dream_stream_bot.config.BotConfig;
import com.example.dream_stream_bot.service.MessageHandlerService;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.ActionType;
import org.telegram.telegrambots.meta.api.methods.send.SendChatAction;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

@Component
@AllArgsConstructor
public class TelegramChatBot extends TelegramLongPollingBot {

    private static final Logger LOGGER = LoggerFactory.getLogger(TelegramChatBot.class);

    private static final int CHARACTERS_PER_SECOND = 100;

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
        handleUpdateAsync(update);
    }

    @Async
    public void handleUpdateAsync(Update update) {
        try {
            if (update.hasMessage() && update.getMessage().hasText()) {
                Message message = update.getMessage();
                User user = message.getFrom();
                LOGGER.info(message.toString());
                LOGGER.info("Message from {} [{}]: {}", user.getUserName(), user.getFirstName(), message.getText());

                // Персональное сообщение
                if (message.getChat().isUserChat()) {
                    var sendMessageList = switch (message.getText()) {
                        case "/start" -> messageHandlerService.start(message);
                        case "/help" -> messageHandlerService.help(message);
                        // Персональное сообщение
                        default -> messageHandlerService.handlePersonalMessage(message);
                    };

                    if (sendMessageList != null) {
                        LOGGER.info("Response from {} [{}]: {}", user.getFirstName(), user.getUserName(), sendMessageList);
                        sendMessageWithTyping(sendMessageList);
                    }

                }

                // Сообщение в группе
                else if (message.getChat().isGroupChat() || message.getChat().isSuperGroupChat()) {

                    List<SendMessage> sendMessageList = null;

                    // Ответ на сообщение бота
                    if (message.isReply() && getBotUsername().equals(message.getReplyToMessage().getFrom().getUserName())) {
                        sendMessageList = messageHandlerService.handleReplyToBotMessage(message);
                    }
                    // Упоминание имени бота
                    else if (containsBotName(message.getText()) || containsTriggers(message.getText())) {
                        sendMessageList = messageHandlerService.handleReplyToBotMessage(message);
                    }

                    // Сообщение в канале
//                    else if (message.getIsAutomaticForward()) {  // проверить null
//                        response = messageHandlerService.handleChannelMessage(message);
//                    }

                    if (sendMessageList != null) {
                        LOGGER.info("Response from {} [{}]: {}", user.getFirstName(), user.getUserName(), sendMessageList);
                        sendMessageWithTyping(sendMessageList);
                    }
                }

            } else if (update.hasCallbackQuery()) {
                var callbackQuery = update.getCallbackQuery();
                long chatId = callbackQuery.getMessage().getChatId();
                var user = callbackQuery.getFrom();

                LOGGER.info("CallbackQuery {} [{}]: {}", user.getFirstName(), user.getUserName(), callbackQuery);

                List<SendMessage> sendMessageList = messageHandlerService.handleCallbackQuery(callbackQuery);

                if (sendMessageList != null) {
                    LOGGER.info("Response from {} [{}]: {}", user.getFirstName(), user.getUserName(), sendMessageList);
                    sendMessageWithTyping(sendMessageList);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error handling update", e);
            if (update.hasMessage()) {
                sendErrorMessage(update.getMessage().getChatId(), "Произошла ошибка. Попробуйте позже.");
            }
        }
    }

    @Async
    public void sendMessageWithTyping(List<SendMessage> sendMessages) {
        for (SendMessage msg : sendMessages) {
            try {
                execute(msg);
            } catch (TelegramApiException e) {
                LOGGER.error("Failed to send message via Telegram API", e);
            }
        }
    }

    public void sendErrorMessage(Long chatId, String text) {
        try {
            SendMessage errorMsg = SendMessage.builder()
                    .chatId(chatId)
                    .text(text)
                    .build();
            execute(errorMsg);
        } catch (TelegramApiException e) {
            LOGGER.error("Failed to send error message", e);
        }
    }

    public boolean containsBotName(String text) {
        return Stream.concat(botConfig.getBotAliasesList().stream(), Stream.of(botConfig.getBotName()))
                .anyMatch(text::contains);
    }

    public boolean containsTriggers(String text) {
        return botConfig.getBotTriggersList().stream().anyMatch(text.toLowerCase(Locale.ROOT)::contains);
    }

    private void sendTypingAction(String chatId, long durationInSeconds) {

        SendChatAction chatAction = new SendChatAction();
        chatAction.setChatId(chatId);
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

    private void sendMessageWithTyping(SendMessage message) {

        int durationInSeconds = message.getText().length() / CHARACTERS_PER_SECOND;
        sendTypingAction(message.getChatId(), durationInSeconds);
        sendMessage(message);
    }

}
