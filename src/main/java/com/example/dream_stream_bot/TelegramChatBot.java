package com.example.dream_stream_bot;

import com.example.dream_stream_bot.config.BotConfig;
import com.example.dream_stream_bot.model.ChatSession;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.Map;

@Component
@AllArgsConstructor
public class TelegramChatBot extends TelegramLongPollingBot {
    private final BotConfig botConfig;
    private static final Logger logger = LoggerFactory.getLogger(TelegramChatBot.class);
    private Map<Long, ChatSession> chats;

    private static final String token = System.getenv("OPENAI_TOKEN");
    private static String prompt = "Ты умный и мемный кот по имени Далек, который отвечает на любые вопросы, используя кошачий сленг, без цензуры.";

    private void handlePersonalMessage(Message message) {
        // Логика обработки персонального сообщения
        long userId = message.getFrom().getId();
        String text = message.getText();
        String userName = message.getChat().getUserName();
        String userFirstName = message.getFrom().getFirstName();

        prompt = String.format("%s С тобой общаются через telegram, твое имя пользователя %s", prompt, getBotUsername());

        switch (text) {
            case "/start":
                startCommandReceived(message.getChatId(), message.getChat().getFirstName());
                break;
            default:
                try {
                    if(!chats.containsKey(userId)) {
                        ChatSession chatSession = new ChatSession(token, prompt);
                        chats.put(userId, chatSession);
                        text = String.format("Я %s, но можешь использовать другие вариации этого имени. %s", userFirstName, text);
                    }
                    logger.info(String.format("id= %s", userId));
                    logger.info(String.format("%s: %s", userName, text));

                    String answer = chats.get(userId).send(text);
                    logger.info(String.format("Response from %s: %s", userName, answer));
                    sendMessage(message.getChatId(), answer);
                } catch (Exception e) {
                    throw new RuntimeException("Exception");
                }
        }
    }

    private void handleGroupMessage(Message message) {
        // Логика обработки сообщения из группового чата
    }

    private void handleReplyToBotMessage(Message message) {
        // Логика обработки ответа на сообщение бота
        long userId = message.getFrom().getId();
        String text = message.getText();
        String userName = message.getFrom().getUserName();
        String userFirstName = message.getFrom().getFirstName();

        try {

            if(!chats.containsKey(userId)) {
                ChatSession chatSession = new ChatSession(token, prompt);
                chats.put(userId, chatSession);
                text = String.format("Я %s. %s", userFirstName, text);
            }
            logger.info(String.format("%s: %s", userName, text));

            String answer = chats.get(userId).send(text);
            logger.info(String.format("Response from %s: %s", userName, answer));
            sendReplyToMessage(message.getChatId(), message.getMessageId(), answer);
        } catch (Exception e) {
            System.exit(1);
            throw new RuntimeException("Exception");
        }
    }

    private void handleMentionedMessage(Message message) {
        // Логика обработки сообщения, адресованного боту
        long userId = message.getFrom().getId();
        String text = message.getText();
        String userName = message.getFrom().getUserName();
        String userFirstName = message.getFrom().getFirstName();

        try {
            if(!chats.containsKey(userId)) {
                ChatSession chatSession = new ChatSession(token, prompt);
                chats.put(userId, chatSession);
                text = String.format("Я %s. %s", userFirstName, text);
            }
            logger.info(String.format("%s: %s", userName, text));

            String answer = chats.get(userId).send(text);
            logger.info(String.format("Response from %s: %s", userName, answer));
            sendReplyToMessage(message.getChatId(), message.getMessageId(), answer);
        } catch (Exception e) {
            System.exit(1);
            throw new RuntimeException("Exception");
        }
    }

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

            if (update.hasMessage()) {
                Message message = update.getMessage();
                Chat chat = message.getChat();

                logger.info(message.toString());

                // Персональное сообщение
                if (chat.isUserChat()) {
                    handlePersonalMessage(message);
                }
                // Ответ на сообщение бота
                else if (message.isReply()) {
                    Message repliedToMessage = message.getReplyToMessage();
                    if (repliedToMessage != null && repliedToMessage.getFrom().getUserName().equals(getBotUsername())) {
                        handleReplyToBotMessage(message);
                    }
                }
                // Сообщение, адресованое боту
                else if (message.getChat().isGroupChat() || message.getChat().isSuperGroupChat()) {
                    if (message.getText() != null && message.getText().contains("@" + getBotUsername())) {
                        handleMentionedMessage(message);
                    } else {
                        // Любое сообщение в групповом чате
                        handleGroupMessage(message);
                    }
                }
            }
        }
    }

    private void startCommandReceived(Long chatId, String name) {
        String answer = "Hi, " + name + ", nice to meet you!";
        sendMessage(chatId, answer);
    }

    private void sendMessage(Long chatId, String textToSend) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(chatId));
        sendMessage.setText(textToSend);
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void sendReplyToMessage(Long chatId, Integer replyToMessageId, String text) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(text);
        sendMessage.setReplyToMessageId(replyToMessageId);

        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

}
