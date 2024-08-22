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
    private final Deque<String> actors = new ArrayDeque<>();
    @Getter
    private final Map<String, String> persons = new HashMap<>();

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

    void putPerson(String key, String value) {
        persons.put(key, value);
    }

    boolean addAllElements(List<String> elements) {
        return this.elements.addAll(elements);
    }

    boolean addAllActors(List<String> elements) {
        return this.actors.addAll(elements);
    }

    String pollFirstElement() {
        return elements.pollFirst();
    }

    String pollFirstActor() {
        return actors.pollFirst();
    }

    String elementsToString() {
        return String.join("\n", elements);
    }

    String actorsToString() {
        return String.join("\n", actors);
    }

    public String associationsCollectToString() {
        return associations.entrySet().stream()
                .map(entry -> String.format("**%s** - %s", entry.getKey(), entry.getValue()))
                .collect(Collectors.joining("\n"));
    }

    public String personsCollectToString() {
            return persons.entrySet().stream()
                    .map(entry -> String.format("**%s** - %s", entry.getKey(), entry.getValue()))
                    .collect(Collectors.joining("\n"));
        }
}
