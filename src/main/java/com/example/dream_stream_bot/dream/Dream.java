package com.example.dream_stream_bot.dream;

import lombok.Getter;

import java.util.*;
import java.util.stream.Collectors;

public class Dream {
    private final StringBuilder history = new StringBuilder();

    @Getter
    private final List<DreamElement> associations = new ArrayList<>();
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

    boolean addAllElements(List<DreamElement> elements) {
        return this.associations.addAll(elements);
    }

    boolean addAllActors(List<DreamActor> elements) {
        return this.actors.addAll(elements);
    }

    public String associationsCollectToString() {
        return associations.stream()
                .map(entry -> String.format("**%s** - %s", entry.getName(), entry.getAssociation()))
                .collect(Collectors.joining("\n"));
    }

    public String personsCollectToString() {
        return actors.stream()
                .map(a -> String.format("**%s** - %s", a.getPerson(), a.getCharacteristic()))
                .collect(Collectors.joining("\n"));
    }
}
