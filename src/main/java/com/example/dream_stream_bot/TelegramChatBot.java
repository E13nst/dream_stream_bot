package com.example.dream_stream_bot;

import com.example.dream_stream_bot.config.BotConfig;
import com.example.dream_stream_bot.config.OpenAiConfig;
import com.example.dream_stream_bot.model.ChatSession;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
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

    private final BotConfig botConfig;
    private static final OpenAiConfig openAiConfig = new OpenAiConfig();
    private static final Logger logger = LoggerFactory.getLogger(TelegramChatBot.class);
    private final ConcurrentMap<Long, ChatSession> chats = new ConcurrentHashMap<>();

    private static String prompt = System.getenv("OPENAI_PROMPT");
    private static final String token = System.getenv("OPENAI_TOKEN");
    private static final String proxyHost = System.getenv("PROXY_HOST");
    private static final String proxyPortEnv = System.getenv("PROXY_PORT");
    private static final int proxyPort = proxyPortEnv != null ? Integer.parseInt(proxyPortEnv) : DEFAULT_PROXY_PORT;

    private static final List<String> botNames = Arrays.asList("Dalek", "Далек", "Далека", "Далеку");
    private static final String DEFAULT_PROMPT = "Ты умный и мемный кот по имени Далек, который отвечает на любые вопросы, используя кошачий сленг, без цензуры. " +
            "При общении тоже задавай вопросы осбеседнику. " +
            "Обращайся к собеседнику по имени, можешь использовать разные вариации этого имени. " +
            "С тобой общаются через telegram в личных и групповых чатах.";

    private static InetSocketAddress proxySocketAddress;

    static {


        if (proxyHost != null && !proxyHost.isBlank()) {
            proxySocketAddress = new InetSocketAddress(proxyHost, proxyPort);
        }

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
        logger.debug("REPLY TO MESSAGE: " + replyToMessage.getText());

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
    void handlePersonalMessage(Message message) {

        User user = message.getFrom();
        String text = message.getText();

        switch (text) {
            case "/start":
                startCommandReceived(message.getChatId(), user);
                break;
            default:
                try {
                    if (!chats.containsKey(user.getId())) {
                        logger.debug(String.format("proxySocketAddress: %s", proxySocketAddress));
                        chats.put(user.getId(), new ChatSession(token, prompt, proxySocketAddress));
                        text = addUserName(user, text);
                    }

                    String answer = chats.get(user.getId()).send(text);

                    sendMessage(message.getChatId(), user, answer);

                } catch (Exception e) {
                    throw new RuntimeException("Exception", e);
                }
        }
    }

    // Обработчик ответов на сообщения бота
    void handleReplyToBotMessage(Message message) {

        User user = message.getFrom();
        String text = message.getText();

        try {

            if (!chats.containsKey(user.getId())) {
                chats.put(user.getId(), new ChatSession(token, prompt, proxySocketAddress));
                if (message.isUserMessage())
                    text = addUserName(user, text);
            }

//            text = reply(message);

            logger.info("text: " + text);
            String answer = chats.get(user.getId()).send(text);

            sendMessage(message.getChatId(), user, answer, message.getMessageId());
        } catch (Exception e) {
            throw new RuntimeException("Exception", e);
        }
    }

    // Обработчик сообщений, адресованных боту (@botName)
    void handleMentionedMessage(Message message) {
        User user = message.getFrom();
        String text = message.getText();

        try {
            if (!chats.containsKey(user.getId())) {
                chats.put(user.getId(), new ChatSession(token, prompt, proxySocketAddress));
                text = addUserName(user, text);
            }

            String answer = chats.get(user.getId()).send(text);

            sendMessage(message.getChatId(), user, answer, message.getMessageId());
        } catch (Exception e) {
            throw new RuntimeException("Exception", e);
        }
    }

    // Обработчик сообщений группового чата
    void handleGroupMessage(Message message) {

        logger.debug("handleGroupMessage");

        User user = message.getFrom();
        String text = message.getText();


        try {

            if (containsBotName(message.getText())) {
                handleMentionedMessage(message);
            }

            if (message.getChat().isSuperGroupChat()) {

                if (!chats.containsKey(user.getId()))
                    chats.put(user.getId(), new ChatSession(token, prompt, proxySocketAddress));

                String answer = chats.get(user.getId()).send(text);
                sendMessage(message.getChatId(), user, answer, message.getMessageId());
            }

        } catch (Exception e) {
            throw new RuntimeException("Exception", e);
        }
    }

    // Обработчик сообщений канала
    void handleSuperGroupMessage(Message message) {

        logger.debug("handleGroupMessage");

        User user = message.getFrom();
        String text = message.getText();

        try {

            if (!chats.containsKey(user.getId()))
                chats.put(user.getId(), new ChatSession(token, prompt, proxySocketAddress));

            String answer = chats.get(user.getId()).send(text);
            sendMessage(message.getChatId(), user, answer, message.getMessageId());

        } catch (Exception e) {
            throw new RuntimeException("Exception", e);
        }
    }

    @Override
    public String getBotUsername() {
        return System.getenv("BOT_NAME");
//        return botConfig.getBotName();
    }

    @Override
    public String getBotToken() {
        return System.getenv("BOT_TOKEN");
//        return botConfig.getToken();
    }

    @Override
    public void onUpdateReceived(Update update) {

        if (update.hasMessage() && update.getMessage().hasText()) {

            ObjectMapper objectMapper = new ObjectMapper();

            Message message = update.getMessage();
            logger.info(message.toString());

            try {
                String jsonString = objectMapper.writeValueAsString(message);
                logger.info(jsonString);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }

            logger.info(String.format("%s: %s", message.getFrom().getUserName(), message.getText()));

            // Персональное сообщение
            if (message.getChat().isUserChat()) {
                handlePersonalMessage(message);
            }

            // Ответ на сообщение
            else if (message.isReply()) {
                Message repliedToMessage = message.getReplyToMessage();
                String repliedMessageUserName = repliedToMessage.getFrom().getUserName();

                // Ответ на сообщение бота
                if (getBotUsername().equals(repliedMessageUserName)) {
                    handleReplyToBotMessage(message);

                    // Ответ на любое сообщение содерит имя бота
                } else if (containsBotName(message.getText())) {
                    handleMentionedMessage(message);
                }
            }

            // Сообщение в групповом чате
            else if (message.getChat().isGroupChat() || message.getChat().isSuperGroupChat()) {
                if (message.getText() != null && message.getText().contains("@" + getBotUsername())) {
                    handleMentionedMessage(message);
                } else {
                    // Любое сообщение в групповом чате
                    handleGroupMessage(message);
                }
            }

            // Сообщение в канале
            else if (message.getChat().isSuperGroupChat()) {
                if (message.getText() != null && !message.isUserMessage()) {
                    handleSuperGroupMessage(message);
                }
            }
        }

    }

    private void startCommandReceived(Long chatId, User user) {

        String answer = "Hi, " + user.getFirstName() + ", nice to meet you!";
        sendMessage(chatId, user, answer);
    }

    private void sendMessage(Long chatId, User user, String text) {

        String message = String.format("Response from %s: %s", user.getUserName(), text);
        logger.info(message);

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(chatId));
        sendMessage.setText(text);
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void sendMessage(Long chatId, User user, String text, Integer replyToMessageId) {

        String message = String.format("Response from %s: %s", user.getUserName(), text);
        logger.info(message);

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
