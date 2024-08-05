package com.example.dream_stream_bot;

import com.example.dream_stream_bot.config.BotConfig;
import com.example.dream_stream_bot.model.ChatSession;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.ActionType;
import org.telegram.telegrambots.meta.api.methods.send.SendChatAction;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
@AllArgsConstructor
public class TelegramChatBot extends TelegramLongPollingBot {

    private static final int DEFAULT_PROXY_PORT = 1337;
    private static final int NOT_REPLY = 0;
    private static final int CHARACTERS_PER_SECOND = 20;
    private static final Logger LOGGER = LoggerFactory.getLogger(TelegramChatBot.class);

    private final BotConfig botConfig;
    private final ConcurrentMap<Long, ChatSession> chats = new ConcurrentHashMap<>();

    private static String prompt = System.getenv("OPENAI_PROMPT");

    private static final List<String> botNames = Arrays.asList("Dalek", "Далек", "Далека", "Далеку");
    private static final String DEFAULT_PROMPT = "Ты умный и мемный кот по имени Далек, который отвечает на любые вопросы, используя кошачий сленг, без цензуры. " +
            "При общении тоже задавай вопросы осбеседнику. " +
            "Обращайся к собеседнику по имени, можешь использовать разные вариации этого имени. " +
            "С тобой общаются через telegram в личных и групповых чатах.";

    static {

        if (prompt == null || prompt.isBlank()) {
            prompt = DEFAULT_PROMPT;
        }
    }

    public static boolean containsBotName(String text) {
        return botNames.stream().anyMatch(text::contains);
    }

    public static String addUserName(User user, String text) {
        return String.format("Я %s. %s", user.getFirstName(), text);
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

    // Обработчик персональных сообщений
    String handlePersonalMessage(Message message) {

        String openaiToken = botConfig.getOpenaiToken();

        User user = message.getFrom();
        String text = message.getText();

        switch (text) {
            case "/start":
                return startCommandReceived(message.getChatId(), user);
            default:
                if (!chats.containsKey(user.getId())) {
                    LOGGER.debug(String.format("proxySocketAddress: %s", botConfig.getProxySocketAddress()));
                    chats.put(user.getId(), new ChatSession(openaiToken, prompt, botConfig.getProxySocketAddress()));
                    text = addUserName(user, text);
                }

                return chats.get(user.getId()).send(text);
        }
    }

    // Обработчик ответов на сообщения бота
    String handleReplyToBotMessage(Message message) {

        String openaiToken = botConfig.getOpenaiToken();

        User user = message.getFrom();
        String text = message.getText();

        if (!chats.containsKey(user.getId())) {
            chats.put(user.getId(), new ChatSession(openaiToken, prompt, botConfig.getProxySocketAddress()));
            text = addUserName(user, text);
        }

//            text = reply(message);

        LOGGER.info("text: " + text);
        return chats.get(user.getId()).send(text);
    }

    // Обработчик сообщений, адресованных боту (@botName)
    String handleMentionedMessage(Message message) {

        String openaiToken = botConfig.getOpenaiToken();

        User user = message.getFrom();
        String text = message.getText();

        if (!chats.containsKey(user.getId())) {
            chats.put(user.getId(), new ChatSession(openaiToken, prompt, botConfig.getProxySocketAddress()));
            text = addUserName(user, text);
        }

        return chats.get(user.getId()).send(text);

    }

    // Обработчик сообщений группового чата
    String handleGroupMessage(Message message) {

        String openaiToken = botConfig.getOpenaiToken();

        User user = message.getFrom();
        String text = message.getText();


        if (containsBotName(message.getText())) {
            return handleMentionedMessage(message);
        } else if (message.getChat().isSuperGroupChat()) {

            if (!chats.containsKey(user.getId()))
                chats.put(user.getId(), new ChatSession(openaiToken, prompt, botConfig.getProxySocketAddress()));

            return chats.get(user.getId()).send(text);

        } else return "";

    }

    // Обработчик сообщений канала
    String handleSuperGroupMessage(Message message) {

        String openaiToken = botConfig.getOpenaiToken();

        User user = message.getFrom();
        String text = message.getText();

        if (!chats.containsKey(user.getId()))
            chats.put(user.getId(), new ChatSession(openaiToken, prompt, botConfig.getProxySocketAddress()));

        return chats.get(user.getId()).send(text);
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

            String responseText = null;
            ObjectMapper objectMapper = new ObjectMapper();

            Message message = update.getMessage();
            Integer replyToMessageId = message.getMessageId();
            User user = message.getFrom();

            LOGGER.info(message.toString());

            try {
                String jsonString = objectMapper.writeValueAsString(message);
                LOGGER.info(jsonString);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }

            LOGGER.info(String.format("%s: %s", message.getFrom().getUserName(), message.getText()));

            // Персональное сообщение
//            if (message.getChat().isUserChat()) {
//                replyToMessageId = NOT_REPLY;
//                responseText = handlePersonalMessage(message);
//            } else if (message.isReply() && getBotUsername().equals(message.getReplyToMessage().getFrom().getUserName())) {
//                responseText = handleReplyToBotMessage(message);
//            } else if (containsBotName(message.getText())) {
//                responseText = handleMentionedMessage(message);
//            } else if (message.getChat().isGroupChat()) {
//                responseText = handleGroupMessage(message);
//            } else if (message.getChat().isSuperGroupChat() && !message.isUserMessage()) {
//                responseText = handleSuperGroupMessage(message);
//            }

            // Персональное сообщение
            if (message.getChat().isUserChat()) {
                replyToMessageId = NOT_REPLY;
                responseText = handlePersonalMessage(message);
            }

            // Ответ на сообщение
            else if (message.isReply()) {
                Message repliedToMessage = message.getReplyToMessage();
                String repliedMessageUserName = repliedToMessage.getFrom().getUserName();

                // Ответ на сообщение бота
                if (getBotUsername().equals(repliedMessageUserName)) {
                    responseText = handleReplyToBotMessage(message);

                    // Ответ на любое сообщение содерит имя бота
                } else if (containsBotName(message.getText())) {
                    responseText = handleMentionedMessage(message);
                }
            }

            // Сообщение в групповом чате
            else if (message.getChat().isGroupChat()) {
                if (message.getText() != null && message.getText().contains("@" + getBotUsername())) {
                    responseText = handleMentionedMessage(message);
                } else {
                    // Любое сообщение в групповом чате
                    responseText = handleGroupMessage(message);
                }
            }

            // Сообщение в канале
            else if (message.getChat().isSuperGroupChat()) {
                if (message.getText() != null && !message.isUserMessage()) {
                    responseText = handleSuperGroupMessage(message);
                }
            }

            if (responseText != null) {
                Long finalChatId = message.getChatId();
                String finalResponseText = responseText;
                Integer finalReplyToMessageId = replyToMessageId;
                new Thread(() -> sendMessageWithTyping(finalChatId, user, finalResponseText, finalReplyToMessageId)).start();
            }
        }

    }

    private String startCommandReceived(Long chatId, User user) {

        return "Hi, " + user.getFirstName() + ", nice to meet you!";
//        sendMessage(chatId, user, answer);
    }

    private void sendTypingAction(Long chatId, long durationInSeconds) {

        SendChatAction chatAction = new SendChatAction();
        chatAction.setChatId(chatId.toString());
        chatAction.setAction(ActionType.TYPING);

        try {
            long endTime = System.currentTimeMillis() + durationInSeconds * 1000;
            while (System.currentTimeMillis() < endTime) {
                execute(chatAction); // Выполнение отправки действия
                Thread.sleep(5000);  // Задержка 5 секунд перед отправкой следующего действия
            }
        } catch (TelegramApiException e) {
            LOGGER.error("Telegram API exception occurred", e);
        } catch (InterruptedException e) {
            LOGGER.error("Thread was interrupted", e);
            Thread.currentThread().interrupt();
        }
    }

    private void sendMessage(Long chatId, User user, String text, Integer replyToMessageId) {

        LOGGER.info(String.format("Response from %s: %s", user.getUserName(), text));

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
        sendTypingAction(chatId, durationInSeconds);
        sendMessage(chatId, user, responseText, replyToMessageId);
    }

}
