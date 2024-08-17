package com.example.dream_stream_bot.dream;

import com.example.dream_stream_bot.model.ChatSession;
import lombok.Getter;

import java.util.*;

public class DreamAnalyzer {

    private final ChatSession chat;
    private final String userName;
    private final StringBuilder history = new StringBuilder();

    private AnalyzerState state;
    @Getter
    private final Map<String, String> objects = new HashMap<>();
    @Getter
    private final Map<String, String> actors = new HashMap<>();

    public DreamAnalyzer(ChatSession chat, String userName) {
        this.chat = chat;
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

    public DreamStatus getCurrentState() {
        return state.getCurrentState();
    }

    public String execute(String text) {
            return state.execute(this, text);
    }

    public String init() {
        return state.init(this);
    }

//    public String getResult() {
//        return state.getResult(this);
//    }

    public void setObjectList(List<String> list) {
        for (String element : list) {
            objects.put(element, "");
        }
    }

    public void setAssociation(String key, String value) {
        objects.put(key, value);
    }

    public List<String> getObjectList() {
        return new ArrayList<>(objects.keySet());
    }

    public void setActorsList(List<String> list) {
        for (String element : list) {
            actors.put(element, "");
        }
    }

    public List<String> getActorsList() {
        return new ArrayList<>(actors.keySet());
    }

    public String extractItems(String prompt) {
        String query = String.format("%s \n\n%s", prompt, history);
        return chat.send(query, userName);
    }

    public static List<String> extractAndSplit(String input) {
        int startIndex = input.indexOf('[') + 1;
        int endIndex = input.lastIndexOf(']');

        if (startIndex > 0 && endIndex > startIndex) {
            String extracted = input.substring(startIndex, endIndex);
            List<String> list = Arrays.asList(extracted.split(","));
            // Удаление пробелов вокруг элементов списка
            list = list.stream().map(e -> e.replace("\"", "")).map(String::trim).toList();
            return list;
        } else {
//            LOGGER.error("Brackets not found or incorrect order.");
            return List.of(); // Возвращаем пустой список в случае ошибки
        }
    }

    public String analyze() {
        String prompt = "Интерпретируй этот сон по Юнгу, операясь на мои личные ассоциации:\n";

        StringBuilder query = new StringBuilder();
        query.append(prompt);

        for (Map.Entry<String, String> entry : objects.entrySet()) {
            query.append(entry.getKey()).append(" - ").append(entry.getValue()).append("\n");
        }
        for (Map.Entry<String, String> entry : actors.entrySet()) {
            query.append(entry.getKey()).append(" - ").append(entry.getValue()).append("\n");
        }
        return chat.send(query.toString(), userName);
    }
}
