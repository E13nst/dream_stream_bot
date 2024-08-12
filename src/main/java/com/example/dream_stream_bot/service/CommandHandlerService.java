package com.example.dream_stream_bot.service;

import com.example.dream_stream_bot.config.BotConfig;
import com.example.dream_stream_bot.model.*;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

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

    private static final InlineKeyboardMarkup keyboardMarkup = new CommandKeyboardNew()
            .addKey("Next", Buttons.NEXT.toString())
            .addKey("Previous", Buttons.PREVIOUS.toString())
            .build();

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

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId());
        sendMessage.setText(response);
        sendMessage.setReplyMarkup(keyboardMarkup);
        return sendMessage;
    }

    public SendMessage help(long chatId) {

        String response = "Hi, nice to meet you!";

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(response);
        return sendMessage;
    }

    public SendMessage next(CallbackQuery query) {

        String response = "Hi, " + query.getFrom().getFirstName() + ", this is next handler!";

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(query.getMessage().getChatId());
        sendMessage.setText(response);
        sendMessage.setReplyMarkup(keyboardMarkup);
        return sendMessage;
    }

    public SendMessage previous(CallbackQuery query) {

        String response = "Hi, " + query.getFrom().getFirstName() + ", this is previous handler!";

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(query.getMessage().getChatId());
        sendMessage.setText(response);
        sendMessage.setReplyMarkup(keyboardMarkup);
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
