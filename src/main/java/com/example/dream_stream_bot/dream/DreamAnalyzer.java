package com.example.dream_stream_bot.dream;

import com.example.dream_stream_bot.model.ChatSession;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.util.*;
import java.util.stream.Collectors;

public class DreamAnalyzer {

    private static final Logger LOGGER = LoggerFactory.getLogger(DreamAnalyzer.class);

    private final ChatSession openaiChat;
    private final String userName;
    private final StringBuilder history = new StringBuilder();

    @Getter
    private final long telegramChatId;

    private AnalyzerState state;
    @Getter
    private final Map<String, String> associations = new HashMap<>();
    @Getter
    private final Map<String, String> persons = new HashMap<>();

    public DreamAnalyzer(ChatSession openaiChat, String userName, long telegramChatId) {
        this.openaiChat = openaiChat;
        this.telegramChatId = telegramChatId;
        this.userName = userName;
        this.state = new DreamNew();
    }

    public void setState(AnalyzerState state) {
        this.state = state;
    }

    public void next() {
        state.next(this);
    }

    public void previous() {
        state.prev(this);
    }

    public void addHistory(String text) {
        history.append(text).append("\n");
    }

    public String getHistory() {
        return history.toString();
    }

    public DreamStatus getState() {
        return state.getState();
    }

    public List<SendMessage> execute(String text) {
            return state.execute(this, text);
    }

    public List<SendMessage> init() {
        return state.init(this);
    }

    public void putAssociation(String key, String value) {
        associations.put(key, value);
    }

    public List<String> extractItemsAndSplit(String prompt) {
        String query = String.format("%s %s", prompt, history);
        String response = openaiChat.send(query, userName);

        int startIndex = response.indexOf('[') + 1;
        int endIndex = response.lastIndexOf(']');

        if (startIndex > 0 && endIndex > startIndex) {
            String extracted = response.substring(startIndex, endIndex);
            List<String> list = Arrays.asList(extracted.split(","));
            list = list.stream().map(e -> e.replace("\"", "")).map(String::trim).toList();
            return list;
        } else {
            LOGGER.error("Brackets not found or incorrect order.");
            return List.of();
        }
    }

    public String analyze() {
        String prompt = "Интерпретируй это сновидения по Юнгу, операясь на мои личные ассоциации:\n" +
                "%s\n" +
                "И персонажей моего сновидения, которые могут представлять мою персону, тень, аниму или анимуса:\n" +
                "%s\n" +
                "Учитывай взаимодейстаие этих персонажей и объектов между собой в контексте сновидения";

        String query = String.format(prompt,
                associations.entrySet().stream().map(entry -> entry.getKey() + " - " + entry.getValue())
                        .collect(Collectors.joining("\n")),
                persons.entrySet().stream().map(entry -> entry.getKey() + " - " + entry.getValue())
                        .collect(Collectors.joining("\n"))
        );

        return openaiChat.send(query, userName);
    }

    public SendMessage newTelegramMessage(String text) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(getTelegramChatId());
        sendMessage.setText(text);
        return sendMessage;
    }
}
