package com.example.dream_stream_bot.bot;

import com.example.dream_stream_bot.config.BotConfig;
import com.example.dream_stream_bot.service.telegram.MessageHandlerService;
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
                handleTextMessage(update.getMessage());
            } else if (update.hasCallbackQuery()) {
                handleCallbackQuery(update.getCallbackQuery());
            }
        } catch (Exception e) {
            LOGGER.error("❌ Error handling update ID: {} | Error: {} | Stack trace: {}", 
                update.getUpdateId(), e.getMessage(), e.getStackTrace()[0]);
            LOGGER.error("❌ Full stack trace:", e);
            if (update.hasMessage()) {
                sendErrorMessage(update.getMessage().getChatId(), 
                    "Произошла ошибка: " + e.getClass().getSimpleName() + ". Попробуйте позже.");
            }
        }
    }

    private void handleTextMessage(Message message) {
        User user = message.getFrom();
        String chatType = getChatType(message.getChat());
        
        LOGGER.info("📨 Received message | User: {} (@{}) | Chat: {} | Text: '{}'", 
            user.getFirstName(), user.getUserName(), chatType, message.getText());

        List<SendMessage> response = null;

        if (message.getChat().isUserChat()) {
            response = handlePersonalMessage(message);
        } else if (message.getChat().isGroupChat() || message.getChat().isSuperGroupChat()) {
            response = handleGroupMessage(message);
        }

        if (response != null && !response.isEmpty()) {
            LOGGER.info("📤 Sending response | User: {} (@{}) | Messages: {}", 
                user.getFirstName(), user.getUserName(), response.size());
            sendMessageWithTyping(response);
        }
    }

    private void handleCallbackQuery(org.telegram.telegrambots.meta.api.objects.CallbackQuery callbackQuery) {
        User user = callbackQuery.getFrom();
        String data = callbackQuery.getData();
        
        LOGGER.info("🔘 Callback query | User: {} (@{}) | Data: '{}'", 
            user.getFirstName(), user.getUserName(), data);

        List<SendMessage> response = messageHandlerService.handleCallbackQuery(callbackQuery);

        if (response != null && !response.isEmpty()) {
            LOGGER.info("📤 Sending callback response | User: {} (@{}) | Messages: {}", 
                user.getFirstName(), user.getUserName(), response.size());
            sendMessageWithTyping(response);
        }
    }

    private List<SendMessage> handlePersonalMessage(Message message) {
        return switch (message.getText()) {
            case "/start" -> messageHandlerService.start(message);
            case "/help" -> messageHandlerService.help(message);
            default -> messageHandlerService.handlePersonalMessage(message);
        };
    }

    private List<SendMessage> handleGroupMessage(Message message) {
        // Ответ на сообщение бота
        if (message.isReply() && getBotUsername().equals(message.getReplyToMessage().getFrom().getUserName())) {
            return messageHandlerService.handleReplyToBotMessage(message);
        }
        // Упоминание имени бота
        else if (containsBotName(message.getText()) || containsTriggers(message.getText())) {
            return messageHandlerService.handleReplyToBotMessage(message);
        }
        
        return null;
    }

    private String getChatType(org.telegram.telegrambots.meta.api.objects.Chat chat) {
        if (chat.isUserChat()) return "private";
        if (chat.isGroupChat()) return "group";
        if (chat.isSuperGroupChat()) return "supergroup";
        if (chat.isChannelChat()) return "channel";
        return "unknown";
    }

    @Async
    public void sendMessageWithTyping(List<SendMessage> sendMessages) {
        for (SendMessage msg : sendMessages) {
            try {
                int durationInSeconds = Math.max(1, msg.getText() != null ? msg.getText().length() / CHARACTERS_PER_SECOND : 1);
                sendTypingAction(msg.getChatId(), durationInSeconds);
                execute(msg);
                LOGGER.debug("✅ Message sent successfully | Chat: {} | Text: '{}'", 
                    msg.getChatId(), truncateText(msg.getText(), 50));
            } catch (TelegramApiException e) {
                LOGGER.error("❌ Failed to send message | Chat: {} | Error: {}", 
                    msg.getChatId(), e.getMessage());
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
            LOGGER.info("⚠️ Error message sent | Chat: {}", chatId);
        } catch (TelegramApiException e) {
            LOGGER.error("❌ Failed to send error message | Chat: {} | Error: {}", 
                chatId, e.getMessage());
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
            LOGGER.error("❌ Failed to send typing action | Chat: {} | Error: {}", 
                chatId, e.getMessage());
        } catch (InterruptedException e) {
            LOGGER.error("❌ Typing action interrupted | Chat: {}", chatId);
            Thread.currentThread().interrupt();
        }
    }

    private void sendMessage(SendMessage message) {
        try {
            execute(message);
            LOGGER.debug("✅ Message sent | Chat: {} | Text: '{}'", 
                message.getChatId(), truncateText(message.getText(), 50));
        } catch (TelegramApiException e) {
            LOGGER.error("❌ Failed to send message | Chat: {} | Error: {}", 
                message.getChatId(), e.getMessage());
        }
    }

    private void sendMessageWithTyping(SendMessage message) {
        int durationInSeconds = message.getText().length() / CHARACTERS_PER_SECOND;
        sendTypingAction(message.getChatId(), durationInSeconds);
        sendMessage(message);
    }

    private String truncateText(String text, int maxLength) {
        if (text == null) return "null";
        return text.length() <= maxLength ? text : text.substring(0, maxLength) + "...";
    }
}
