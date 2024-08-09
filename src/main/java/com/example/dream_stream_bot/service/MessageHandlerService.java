package com.example.dream_stream_bot.service;

import com.example.dream_stream_bot.config.BotConfig;
import com.example.dream_stream_bot.model.ChatSession;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;

@Service
public class MessageHandlerService {

    @Autowired
    private FileReadingService fileReadingService;
    @Autowired
    private BotConfig botConfig;

    private static final Logger LOGGER = LoggerFactory.getLogger(MessageHandlerService.class);
    private static final String DEFAULT_PROMPT = "You are an assistant that answers using the local slang of the given place, uncensored.";
    private static final String PROMPT_FILE_NAME = "file:./prompt.txt";

    private final ConcurrentMap<Long, ChatSession> chats = new ConcurrentHashMap<>();

    private String prompt;
    private String openaiToken;
    private String botName;

    private InetSocketAddress proxySocketAddress;

    private List<String> botNameAliases;

    @PostConstruct
    public void init() {
        prompt = getPrompt();
        openaiToken = botConfig.getOpenaiToken();
        botName = botConfig.getBotName();
        proxySocketAddress = botConfig.getProxySocketAddress();
        botNameAliases = botConfig.getBotAliasesList();
    }

    public SendMessage handlePersonalMessage(Message message) {

        User user = message.getFrom();
        ChatSession chatSession = chats.computeIfAbsent(user.getId(), id -> new ChatSession(openaiToken, prompt, proxySocketAddress));
        String query = chats.containsKey(user.getId()) ? message.getText() : addUserName(user, message.getText());
        String response = chatSession.send(query);

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId());
        sendMessage.setText(response);
        return sendMessage;
    }

    // Обработчик ответов на сообщения бота
    public SendMessage handleReplyToBotMessage(Message message) {

        User user = message.getFrom();
        ChatSession chatSession = chats.computeIfAbsent(user.getId(), id -> new ChatSession(openaiToken, prompt, proxySocketAddress));
        String query = chats.containsKey(user.getId()) ? message.getText() : addUserName(user, message.getText());
        String response = chatSession.send(query);

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId());
        sendMessage.setText(response);
        sendMessage.setReplyToMessageId(message.getMessageId());
        return sendMessage;
    }

    // Обработчик ответов на сообщения бота
    public SendMessage handleBotMentionMessage(Message message) {

        User user = message.getFrom();
        ChatSession chatSession = chats.computeIfAbsent(user.getId(), id -> new ChatSession(openaiToken, prompt, proxySocketAddress));
        String query = chats.containsKey(user.getId()) ? message.getText() : addUserName(user, message.getText());
        String response = chatSession.send(query);

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId());
        sendMessage.setText(response);
        sendMessage.setReplyToMessageId(message.getMessageId());
        return sendMessage;
    }

    // Обработчик сообщений канала
    public SendMessage handleSuperGroupMessage(Message message) {

        User user = message.getFrom();
        ChatSession chatSession = chats.computeIfAbsent(user.getId(), id -> new ChatSession(openaiToken, prompt, proxySocketAddress));
        String response = chatSession.send(message.getText());

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId());
        sendMessage.setText(response);
        sendMessage.setReplyToMessageId(message.getMessageId());
        return sendMessage;
    }

    private String getPrompt() {
        try {
            return fileReadingService.readFile(PROMPT_FILE_NAME);
        } catch (IOException e) {
            LOGGER.error("Failed to send message via Telegram API", e);
            return DEFAULT_PROMPT;
        }
    }

    public String startCommandReceived(Long chatId, User user) {

        return "Hi, " + user.getFirstName() + ", nice to meet you!";
//        sendMessage(chatId, user, answer);
    }

    public String helpCommandReceived(Long chatId, User user) {

        return "Hi, " + user.getFirstName() + ", nice to meet you!";
    }

    private static String addUserName(User user, String text) {
        return String.format("Я %s.\n%s", user.getFirstName(), text);
    }

    private static String insertQuote(User user, String text, User rUser, String rText) {
        return String.format("Я %s.\n\n%s писал: \"%s\"\n\n%s",
                user.getFirstName(), rUser.getFirstName(), rText, text);
    }

    private boolean containsBotName(String text) {
        return Stream.concat(botNameAliases.stream(), Stream.of(botName))
                .anyMatch(text::contains);
    }
}
