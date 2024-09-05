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
    @Getter
    Iterator<DreamActor> actorIterator = actors.iterator();

    void addHistory(String text) {
        history.append(text).append("\n");
    }

    public String getHistoryStr() {
        return history.toString();
    }

    void cleanHistory() {
        history.setLength(0);
    }

    void addAllElements(List<DreamElement> elements) {
        this.associations.addAll(elements);
    }

    void addAllActors(List<DreamActor> elements) {
        this.actors.addAll(elements);
        actorIterator = actors.iterator();
    }

    boolean hasActor() {
        return actorIterator.hasNext();
    }

    DreamActor nextActor() {
        return actorIterator.next();
    }

    void initActorsIterator() {
        actorIterator = actors.iterator();
    }

    public String associationsCollectForResult() {
        return associations.stream()
                .map(entry -> String.format("**%s** - %s", entry.getName(), entry.getAssociation()))
                .collect(Collectors.joining("\n"));
    }

    public String personsCollectForResult() {
        return actors.stream()
                .map(a -> String.format("**%s** - %s", a.getName(), a.getCharacteristic()))
                .collect(Collectors.joining("\n"));
    }
    public String contextCollectForResult() {
        return actors.stream()
                .map(a -> String.format("**%s** - %s", a.getCharacteristic(), a.getContext()))
                .collect(Collectors.joining("\n"));
    }

    public String senseCollectForResult() {
        return actors.stream()
                .map(a -> String.format("**%s** - %s", a.getCharacteristic(), a.getSense()))
                .collect(Collectors.joining("\n"));
    }
}
