package com.example.dream_stream_bot.service;

import com.example.dream_stream_bot.config.BotConfig;
import com.example.dream_stream_bot.model.ChatSession;
import jakarta.annotation.PostConstruct;
import net.gcardone.junidecode.Junidecode;
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
        String response = chatSession.send(query, transliterateUserName(user));

        return newTelegramMessage(message.getChatId(), response);
    }

    // Обработчик ответов на сообщения бота
    public SendMessage handleReplyToBotMessage(Message message) {

        ChatSession chatSession = chats.computeIfAbsent(message.getChat().getId(), id -> new ChatSession(openaiToken, prompt, proxySocketAddress));
        String response = chatSession.send(message.getText(), transliterateUserName(message.getFrom()));

//        SendMessage sendMessage = new SendMessage();
//        sendMessage.setChatId(message.getChatId());
//        sendMessage.setText(response);
//        sendMessage.setReplyToMessageId(message.getMessageId());
//        return sendMessage;

        return newTelegramMessage(message.getChatId(), response, message.getMessageId());
    }

    // Обработчик ответов на сообщения бота
    public SendMessage handleBotMentionMessage(Message message) {

        ChatSession chatSession = chats.computeIfAbsent(message.getFrom().getId(), id -> new ChatSession(openaiToken, prompt, proxySocketAddress));
        String response = chatSession.send(message.getText(), message.getFrom().getFirstName());

        return newTelegramMessage(message.getChatId(), response, message.getMessageId());
    }

    // Обработчик сообщений канала
    public SendMessage handleChannelMessage(Message message) {

        User user = message.getFrom();
        ChatSession chatSession = chats.computeIfAbsent(user.getId(), id -> new ChatSession(openaiToken, prompt, proxySocketAddress));
        String response = chatSession.send(message.getText());

        return newTelegramMessage(message.getChatId(), response, message.getMessageId());
    }

    private String getPrompt() {
        try {
            return fileReadingService.readFile(PROMPT_FILE_NAME);
        } catch (IOException e) {
            LOGGER.error("Failed to read the prompt file: {}. Using default prompt.", PROMPT_FILE_NAME, e);            return DEFAULT_PROMPT;
        }
    }

    public static String transliterateUserName(User user) {
        return Junidecode.unidecode(user.getFirstName()).replaceAll("[^a-zA-Z0-9_-]", "");
    }

    private static String addUserName(User user, String text) {
        return String.format("Я %s.\n%s", user.getFirstName(), text);
    }

    private static String insertQuote(User user, String text, User rUser, String rText) {
        return String.format("Я %s.\n\n%s писал: \"%s\"\n\n%s",
                user.getFirstName(), rUser.getFirstName(), rText, text);
    }

    public SendMessage newTelegramMessage(long chatId, String text) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(text);
        return sendMessage;
    }

    public SendMessage newTelegramMessage(long chatId, String text, int replyToMessageId) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(text);
        sendMessage.setReplyToMessageId(replyToMessageId);
        return sendMessage;
    }

}
