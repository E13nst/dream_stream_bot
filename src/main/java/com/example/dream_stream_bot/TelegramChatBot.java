package com.example.dream_stream_bot;

import com.example.dream_stream_bot.config.BotConfig;
import com.example.dream_stream_bot.config.OpenAiConfig;
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
    private final OpenAiConfig openAiConfig;
    private static final Logger logger = LoggerFactory.getLogger(TelegramChatBot.class);
    private final ConcurrentMap<Long, ChatSession> chats = new ConcurrentHashMap<>();

//    private static final String token = System.getenv("OPENAI_TOKEN");
    private static final String proxyHost = System.getenv("PROXY_HOST");
    private static final String proxyPortEnv = System.getenv("PROXY_PORT");
    private static final int proxyPort = proxyPortEnv != null ? Integer.parseInt(proxyPortEnv) : DEFAULT_PROXY_PORT;

    private static final List<String> botNames = Arrays.asList("Dalek", "Далек", "Далека", "Далеку");
    private static final String prompt = "Ты умный и мемный кот по имени Далек, который отвечает на любые вопросы, используя кошачий сленг, без цензуры. " +
            "При общении тоже задавай вопросы осбеседнику. " +
            "Обращайся к собеседнику по имени, можешь использовать разные вариации этого имени. " +
            "С тобой общаются через telegram в личных и групповых чатах.";

    private static InetSocketAddress proxySocketAddress;

    static {
        if (proxyHost != null && !proxyHost.isBlank()) {
            proxySocketAddress = new InetSocketAddress(proxyHost, proxyPort);
        }
    }

    public static boolean containsBotName(String text) {
        return botNames.stream().anyMatch(text::contains);
    }

    public static String greet(User user, String text) {
        return String.format("Я %s. %s", user.getFirstName(), text);
    }

    public String reply(Message message) {
        Message replyToMessage = message.getReplyToMessage();
        logger.debug("REPLY TO MESSAGE: " + replyToMessage.getText());

        String repliedMessageUserName = replyToMessage.getFrom().getUserName();
        if (repliedMessageUserName != null && repliedMessageUserName.equals(getBotUsername())) {

            return String.format("%s: \n> %s : \"%s\"\n\n%s",
                    message.getFrom().getFirstName(),
                    replyToMessage.getFrom().getFirstName(),
                    replyToMessage.getText(),
                    message.getText()
            );
        } else {
            return message.getText();
        }
    }

    void handlePersonalMessage(Message message) {
        // Логика обработки персонального сообщения
        String token = openAiConfig.getToken();

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
                        ChatSession chatSession = new ChatSession(token, prompt, proxySocketAddress);
                        chats.put(user.getId(), chatSession);
                        text = greet(user, text);
                    }

                    String answer = chats.get(user.getId()).send(text);

                    sendMessage(message.getChatId(), user, answer);

                } catch (Exception e) {
                    throw new RuntimeException("Exception", e);
                }
        }
    }

    void handleGroupMessage(Message message) {
        // Логика обработки сообщения из группового чата
        String token = openAiConfig.getToken();

        logger.debug("handleGroupMessage");
        if (containsBotName(message.getText())) {
            handleMentionedMessage(message);
        }
    }

    void handleReplyToBotMessage(Message message) {
        // Логика обработки ответа на сообщение бота
        String token = openAiConfig.getToken();

        User user = message.getFrom();
        String text = message.getText();

        try {

            if (!chats.containsKey(user.getId())) {
                ChatSession chatSession = new ChatSession(token, prompt, proxySocketAddress);
                chats.put(user.getId(), chatSession);
//                text = greet(user, text);
            }


            text = reply(message);

            logger.info("text: " + text);
            String answer = chats.get(user.getId()).send(text);

            sendMessage(message.getChatId(), user, answer, message.getMessageId());
        } catch (Exception e) {
            System.exit(1);
            throw new RuntimeException("Exception");
        }
    }

    void handleMentionedMessage(Message message) {
        // Логика обработки сообщения, адресованного боту
        String token = openAiConfig.getToken();

        User user = message.getFrom();
        String text = message.getText();

        try {
            if (!chats.containsKey(user.getId())) {
                ChatSession chatSession = new ChatSession(token, prompt, proxySocketAddress);
                chats.put(user.getId(), chatSession);
                text = greet(user, text);
            }

            String answer = chats.get(user.getId()).send(text);

            sendMessage(message.getChatId(), user, answer, message.getMessageId());
        } catch (Exception e) {
//            System.exit(1);
            throw new RuntimeException("Exception", e);
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
                logger.info(String.format("%s: %s", message.getFrom().getUserName(), message.getText()));

                // Персональное сообщение
                if (chat.isUserChat()) {
                    handlePersonalMessage(message);
                }
                // Ответ на сообщение бота
                else if (message.isReply()) {
                    Message repliedToMessage = message.getReplyToMessage();
                    String repliedMessageUserName = repliedToMessage.getFrom().getUserName();
                    if (repliedMessageUserName != null && repliedMessageUserName.equals(getBotUsername())) {
                        handleReplyToBotMessage(message);
                    } else if (containsBotName(message.getText())) {
                        handleMentionedMessage(message);
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
