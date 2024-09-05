package com.example.dream_stream_bot.service;

import com.example.dream_stream_bot.config.BotConfig;
import com.example.dream_stream_bot.dream.DreamAnalyzer;
import com.example.dream_stream_bot.model.ChatSession;
import com.example.dream_stream_bot.model.InlineButtons;
import com.example.dream_stream_bot.model.InlineCommandKeyboard;
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

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class CommandHandlerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MessageHandlerService.class);

    @Autowired
    private BotConfig botConfig;
    @Autowired
    private AIService aiService;

    private final ConcurrentMap<Long, ChatSession> chats = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, DreamAnalyzer> dreamAnalyzer = new ConcurrentHashMap<>();

    private String prompt;
    private String openaiToken;

    private InetSocketAddress proxySocketAddress;

    private static final InlineKeyboardMarkup inlineKeyboard = new InlineCommandKeyboard()
            .addKey("Next", InlineButtons.NEXT.toString())
            .addKey("Previous", InlineButtons.PREVIOUS.toString())
            .addKey("Cancel", InlineButtons.CANCEL.toString())
            .build();

    @PostConstruct
    public void init() {
        prompt = botConfig.getPrompt();
        openaiToken = botConfig.getOpenaiToken();
        proxySocketAddress = botConfig.getProxySocketAddress();
    }

    public List<SendMessage> handlePersonalMessage(Message message) {

        List<SendMessage> responseMessageList = new ArrayList<>();
        User user = message.getFrom();
        ChatSession chatSession = chats.computeIfAbsent(user.getId(), id -> new ChatSession(openaiToken, prompt, proxySocketAddress));

        // Обработка логики
        if (dreamAnalyzer.containsKey(user.getId())) {

            var analyzer = dreamAnalyzer.get(user.getId());

            Optional.ofNullable(analyzer.processMessage(message.getText())).ifPresent(responseMessageList::addAll);

            LOGGER.info("STATE: {}", analyzer.getState().toString());

        } else {
            String query = chats.containsKey(user.getId()) ? message.getText() : addUserName(user, message.getText());
            String response = chatSession.send(query, transliterateUserName(user));

            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(message.getChatId());
            sendMessage.setText(response);
            sendMessage.enableMarkdown(true);

            responseMessageList.add(sendMessage);
        }
        return responseMessageList;
    }

    public List<SendMessage> start(Message message) {

        List<SendMessage> responseMessageList = new ArrayList<>();

        var inlineKeyboardMarkup = new InlineCommandKeyboard()
                .addKey("Начать \u2705", InlineButtons.NEXT.toString())
                .addKey("Отмена \u274C", InlineButtons.CANCEL.toString())
                .build();

        User user = message.getFrom();
        long telegramChatId = message.getChatId();

        ChatSession chat = chats.computeIfAbsent(user.getId(), id -> new ChatSession(openaiToken, prompt, proxySocketAddress));
        dreamAnalyzer.put(user.getId(), DreamAnalyzer.builder()
                .openaiChat(chat)
                .userName(transliterateUserName(user))
                .telegramChatId(telegramChatId)
                .build()
        );

        String msgStart = "Привет!\n" +
                "\n" +
                "Я помогу вам глубже понять свои *сны* и раскрыть их скрытые **смыслы**. Сны — это ключ к пониманию ваших " +
                "внутренних переживаний и эмоций, и я здесь, чтобы помочь вам использовать этот ключ. Просто расскажите " +
                "о своем сне, и вместе мы разберемся, что он может означать для вас.\n" +
                "\n" +
                "Мы будем находить ассоциации, которые важны, чтобы понимать, как ваш сон связан с вашими переживаниями, " +
                "чувствами и эмоциями. Нажмите на кнопку, чтобы начать, и я проведу вас по этому пути!";

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(message.getChatId());
        sendMessage.setText(msgStart);
        sendMessage.setReplyMarkup(inlineKeyboardMarkup);
        sendMessage.enableMarkdown(true);

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

        var analyzer = dreamAnalyzer.get(user.getId());
        analyzer.next();
        List<SendMessage> responseMessageList = new ArrayList<>();
        Optional.ofNullable(analyzer.processMessage(""))
                .ifPresent(responseMessageList::addAll);

        return responseMessageList;
    }

    public List<SendMessage> previous(CallbackQuery query) {

        User user = query.getFrom();

        var analyzer = dreamAnalyzer.get(user.getId());
        analyzer.previous();

        List<SendMessage> responseMessageList = new ArrayList<>();
        Optional.ofNullable(analyzer.processMessage(""))
                .ifPresent(responseMessageList::addAll);

        return responseMessageList;
    }

    public List<SendMessage> delete(CallbackQuery query) {

        dreamAnalyzer.remove(query.getFrom().getId());

        List<SendMessage> responseMessageList = new ArrayList<>();
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(query.getMessage().getChatId());
        sendMessage.setText("Анализ завершен");
        responseMessageList.add(sendMessage);

        return responseMessageList;
    }

    private static String addUserName(User user, String text) {
        return String.format("Я %s.\n%s", user.getFirstName(), text);
    }

    public static String chatUserName(User user) {
        return user.getLastName() == null ? user.getFirstName() : user.getFirstName() + " " + user.getLastName();
    }

    public static String transliterateUserName(User user) {
        return Junidecode.unidecode(user.getFirstName().replace(" ", "_"))
                .replaceAll("[^a-zA-Z0-9_-]", "");
    }

}
