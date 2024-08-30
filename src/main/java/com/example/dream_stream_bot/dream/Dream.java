package com.example.dream_stream_bot.dream;

import lombok.Getter;

import java.util.*;
import java.util.stream.Collectors;

public class Dream {
    private final StringBuilder history = new StringBuilder();

    @Getter
    private final Deque<String> elements = new ArrayDeque<>();
    @Getter
    private final Map<String, String> associations = new HashMap<>();
    @Getter
    private final List<DreamActor> actors = new ArrayList<>();

    void addHistory(String text) {
        history.append(text).append("\n");
    }

    String getHistoryStr() {
        return history.toString();
    }

    void cleanHistory() {
        history.setLength(0);
    }

    void putAssociation(String key, String value) {
        associations.put(key, value);
    }

//    boolean addAllElements(List<String> elements) {
//        return this.elements.addAll(elements);
//    }

//    boolean addAllActors(List<DreamActor> elements) {
//        return this.actors.addAll(elements);
//    }

    String pollFirstElement() {
        return elements.pollFirst();
    }

    public String associationsCollectToString() {
        return associations.entrySet().stream()
                .map(entry -> String.format("**%s** - %s", entry.getKey(), entry.getValue()))
                .collect(Collectors.joining("\n"));
    }

    public String personsCollectToString() {
        return actors.stream()
                .map(a -> String.format("**%s** - %s", a.getPerson(), a.getCharacteristic()))
                .collect(Collectors.joining("\n"));
    }
}
