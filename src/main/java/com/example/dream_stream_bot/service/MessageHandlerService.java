package com.example.dream_stream_bot.service;

import com.example.dream_stream_bot.config.BotConfig;
import com.example.dream_stream_bot.model.ChatSession;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
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
//        botNameAliases.add(botConfig.getBotName());
    }

    public String handlePersonalMessage(Message message) {

        User user = message.getFrom();
        String text = message.getText();

        switch (text) {
            case "/start":
                return startCommandReceived(message.getChatId(), user);
            default:
                if (!chats.containsKey(user.getId())) {
                    LOGGER.debug(String.format("proxySocketAddress: %s", proxySocketAddress));
                    chats.put(user.getId(), new ChatSession(openaiToken, prompt, proxySocketAddress));
                    text = addUserName(user, text);
                }

                return chats.get(user.getId()).send(text);
        }
    }

    // Обработчик ответов на сообщения бота
    public String handleReplyToMessage(Message message) {

        User user = message.getFrom();
        String text = message.getText();

        if (!chats.containsKey(user.getId())) {
            String prompt = getPrompt();
            String openaiToken = botConfig.getOpenaiToken();
            chats.put(user.getId(), new ChatSession(openaiToken, prompt, proxySocketAddress));
            text = addUserName(user, text);
        }

        LOGGER.info("text: " + text);
        return chats.get(user.getId()).send(text);
    }

    // Обработчик сообщений канала
    public String handleSuperGroupMessage(Message message) {

        String openaiToken = botConfig.getOpenaiToken();

        User user = message.getFrom();
        String text = message.getText();

        if (!chats.containsKey(user.getId())) {
            String prompt = getPrompt();
            chats.put(user.getId(), new ChatSession(openaiToken, prompt, proxySocketAddress));
        }

        return chats.get(user.getId()).send(text);
    }

    private String getPrompt() {
        try {
            return fileReadingService.readFile(PROMPT_FILE_NAME);
        } catch (IOException e) {
            LOGGER.error("Failed to send message via Telegram API", e);
            return DEFAULT_PROMPT;
        }
    }

    private String startCommandReceived(Long chatId, User user) {

        return "Hi, " + user.getFirstName() + ", nice to meet you!";
//        sendMessage(chatId, user, answer);
    }

    private static String addUserName(User user, String text) {
        return String.format("Я %s. %s", user.getFirstName(), text);
    }

    public boolean containsBotName(String text) {
//        return botNameAliases.stream().allMatch(text::contains);
        return Stream.concat(botNameAliases.stream(), Stream.of(botName))
                .anyMatch(text::contains);
    }
}
