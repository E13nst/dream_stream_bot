package com.example.dream_stream_bot.service;

import com.example.dream_stream_bot.config.BotConfig;
import com.example.dream_stream_bot.model.ChatSession;
import com.example.dream_stream_bot.model.CommandKeyboard;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class CommandHandlerService {

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

    public SendMessage start(Message message) {

        String response = "Hi, " + message.getFrom().getFirstName() + ", nice to meet you!";

        var keyboardMarkup = new CommandKeyboard()
                .addKey("Next")
                .addKey("Previous")
                .build();

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId());
        sendMessage.setText(response);
        sendMessage.setReplyMarkup(keyboardMarkup);
        return sendMessage;
    }

    public SendMessage next(Message message) {

        String response = "Hi, " + message.getFrom().getFirstName() + ", nice to meet you!";

        var keyboardMarkup = new CommandKeyboard()
                .addKey("Next")
                .addKey("Previous")
                .build();

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId());
        sendMessage.setText(response);
        sendMessage.setReplyMarkup(keyboardMarkup);
        return sendMessage;
    }

    public SendMessage previous(Message message) {

        String response = "Hi, " + message.getFrom().getFirstName() + ", nice to meet you!";

        var keyboardMarkup = new CommandKeyboard()
                .addKey("Next")
                .addKey("Previous")
                .build();

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId());
        sendMessage.setText(response);
        sendMessage.setReplyMarkup(keyboardMarkup);
        return sendMessage;
    }

    public SendMessage help(Message message) {

        String response = "Hi, " + message.getFrom().getFirstName() + ", nice to meet you!";

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
            LOGGER.error("Failed to read the prompt file: {}. Using default prompt.", PROMPT_FILE_NAME, e);
            return DEFAULT_PROMPT;
        }
    }
}
