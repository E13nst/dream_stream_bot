package com.example.dream_stream_bot.service;

import com.example.dream_stream_bot.config.BotConfig;
import com.example.dream_stream_bot.dream.DreamAnalyzer;
import com.example.dream_stream_bot.model.*;
import jakarta.annotation.PostConstruct;
import net.gcardone.junidecode.Junidecode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
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
    private final ConcurrentMap<Long, DreamAnalyzer> dreams = new ConcurrentHashMap<>();

    private String prompt;
    private String openaiToken;

    private InetSocketAddress proxySocketAddress;

    private static final InlineKeyboardMarkup inlineKeyboard = new InlineCommandKeyboard()
            .addKey("Next", InlineButtons.NEXT.toString())
            .addKey("Previous", InlineButtons.PREVIOUS.toString())
            .addKey("Cancel", InlineButtons.CANCEL.toString())
            .build();

    private static final ReplyKeyboardMarkup replyKeyboard = new ReplyCommandKeyboard()
            .addKey(ReplyButtons.NEXT.toString())
            .addKey(ReplyButtons.PREVIOUS.toString())
            .addKey(ReplyButtons.CANCEL.toString())
            .build();

    @PostConstruct
    public void init() {
        prompt = getPrompt();
        openaiToken = botConfig.getOpenaiToken();
        proxySocketAddress = botConfig.getProxySocketAddress();
    }

    public List<SendMessage> handlePersonalMessage(Message message) {

        List<SendMessage> responseMessageList = new ArrayList<>();
        User user = message.getFrom();
        ChatSession chatSession = chats.computeIfAbsent(user.getId(), id -> new ChatSession(openaiToken, prompt, proxySocketAddress));

        // Обработка логики
        if (dreams.containsKey(user.getId())) {

            long userId = user.getId();
            var analyzer = dreams.get(userId);

            ReplyButtons value = ReplyButtons.fromTitle(message.getText());

            switch (Objects.requireNonNull(value)) {
                case PREVIOUS -> {
                    analyzer.previous();
                    Optional.ofNullable(analyzer.init())
                            .ifPresent(responseMessageList::addAll);
                    Optional.ofNullable(analyzer.execute(""))
                            .ifPresent(responseMessageList::addAll);
                }
                case NEXT -> {
                    analyzer.next();
                    Optional.ofNullable(analyzer.init())
                            .ifPresent(responseMessageList::addAll);
                    Optional.ofNullable(analyzer.execute(""))
                            .ifPresent(responseMessageList::addAll);
                }
                case CANCEL -> dreams.remove(userId);
                default -> Optional.ofNullable(analyzer.execute(message.getText()))
                        .ifPresent(responseMessageList::addAll);
            }

            LOGGER.info("STATE: {}", analyzer.getState().toString());

        } else {
            String query = chats.containsKey(user.getId()) ? message.getText() : addUserName(user, message.getText());
            String response = chatSession.send(query, transliterateUserName(user));

            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(message.getChatId());
            sendMessage.setText(response);
            sendMessage.setReplyMarkup(replyKeyboard);

            responseMessageList.add(sendMessage);
        }
        return responseMessageList;
    }

    public List<SendMessage> start(Message message) {

        List<SendMessage> responseMessageList = new ArrayList<>();

        User user = message.getFrom();
        long telegramChatId = message.getChatId();

        ChatSession chat = chats.computeIfAbsent(user.getId(), id -> new ChatSession(openaiToken, prompt, proxySocketAddress));
        var dream = dreams.computeIfAbsent(user.getId(), id -> new DreamAnalyzer(chat, transliterateUserName(user), telegramChatId));

        String response = String.format(
                "Привет, %s! Когда будешь готов, нажми кнопку \"%s\", чтобы перейти к анализу.",
                message.getFrom().getFirstName(),
                ReplyButtons.NEXT
        );

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId());
        sendMessage.setText(response);
        sendMessage.setReplyMarkup(replyKeyboard);

        responseMessageList.add(sendMessage);
        return responseMessageList;
    }

    public List<SendMessage> help(long chatId) {

        List<SendMessage> responseMessageList = new ArrayList<>();

        String response = "Hi, nice to meet you!";

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(response);
        responseMessageList.add(sendMessage);
        return responseMessageList;
    }

    public List<SendMessage> next(CallbackQuery query) {

        User user = query.getFrom();
        long telegramChatId = query.getMessage().getChatId();

        var analyzer = dreams.get(user.getId());
        analyzer.next();

        List<SendMessage> responseMessageList = new ArrayList<>();
        Optional.ofNullable(analyzer.init())
                .ifPresent(responseMessageList::addAll);
        Optional.ofNullable(analyzer.execute(""))
                .ifPresent(responseMessageList::addAll);

        return responseMessageList;
    }

    public List<SendMessage> previous(CallbackQuery query) {

        User user = query.getFrom();
        long telegramChatId = query.getMessage().getChatId();

        var analyzer = dreams.get(user.getId());
        analyzer.previous();

        List<SendMessage> responseMessageList = new ArrayList<>();
        Optional.ofNullable(analyzer.init())
                .ifPresent(responseMessageList::addAll);
        Optional.ofNullable(analyzer.execute(""))
                .ifPresent(responseMessageList::addAll);

        return responseMessageList;
    }

    public List<SendMessage> delete(CallbackQuery query) {

//        ChatSession chatSession = chats.computeIfAbsent(query.getFrom().getId(), id -> new ChatSession(openaiToken, prompt, proxySocketAddress));

        dreams.remove(query.getFrom().getId());

        List<SendMessage> responseMessageList = new ArrayList<>();
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(query.getMessage().getChatId());
        sendMessage.setText("Элемент удален");
        responseMessageList.add(sendMessage);

//        sendMessage.setReplyMarkup(keyboardMarkup);
        return responseMessageList;
    }

    private String getPrompt() {
        try {
            return fileReadingService.readFile(PROMPT_FILE_NAME);
        } catch (IOException e) {
            LOGGER.error("Failed to read the prompt file: {}. Using default prompt.", PROMPT_FILE_NAME, e);
            return DEFAULT_PROMPT;
        }
    }

    private static String addUserName(User user, String text) {
        return String.format("Я %s.\n%s", user.getFirstName(), text);
    }

    public static String transliterateUserName(User user) {
        return Junidecode.unidecode(user.getFirstName()).replaceAll("[^a-zA-Z0-9_-]", "");
    }

}
