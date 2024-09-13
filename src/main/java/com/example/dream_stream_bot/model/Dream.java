package com.example.dream_stream_bot.model;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Dream {

    private final StringBuilder history = new StringBuilder();
    @Getter
    private final Map<String, String> associations = new HashMap<>();
    @Getter
    private final List<DreamActor> actors = new ArrayList<>();

    private String currentUnassociatedDreamElement;
    private DreamActor currentActor;
    private DreamState currentState;

    @Getter
    @Setter
    private String interpretation;

    public Dream() {
        this.currentState = DreamState.HISTORY;
    }

    public Dream(DreamState state) {
        this.currentState = state;
    }

    public Dream(String history) {
        this.history.append(history);
        this.currentState = DreamState.HISTORY;
    }

    public void addToHistory(String text) {
        if (!history.isEmpty())
            history.append("\n");
        history.append(text);
    }

    public String getHistory() {
        return history.toString();
    }

    public String getFirstUnassociatedDreamElement() {
        currentUnassociatedDreamElement = associations.entrySet().stream()
                .filter(e -> e.getValue() == null || e.getValue().isEmpty())
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);

        return currentUnassociatedDreamElement;
    }

    public void setCurrentAssociation(String value) {
        if (currentUnassociatedDreamElement != null)
            associations.put(currentUnassociatedDreamElement, value);
    }

    public DreamActor getNextActor() {

        currentActor = switch (currentState) {

            case PERSONALITY -> actors.stream()
                    .filter(a -> a.getCharacteristic() == null || a.getCharacteristic().isEmpty())
                    .findFirst()
                    .orElse(null);

            case CONTEXT -> actors.stream()
                    .filter(a -> a.getContext() == null || a.getContext().isEmpty())
                    .findFirst()
                    .orElse(null);

            case SENSE -> actors.stream()
                    .filter(a -> a.getSense() == null || a.getSense().isEmpty())
                    .findFirst()
                    .orElse(null);

            default -> null;

        };

        return currentActor;
    }

    public DreamActor updateCurrentActor(String value) {

        switch (currentState) {

            case PERSONALITY -> actors.stream()
                    .filter(a -> a.getCharacteristic() == null || a.getCharacteristic().isEmpty())
                    .findFirst()
                    .ifPresent(a -> a.setCharacteristic(value));

            case CONTEXT -> actors.stream()
                    .filter(a -> a.getContext() == null || a.getContext().isEmpty())
                    .findFirst()
                    .ifPresent(a -> a.setContext(value));

            case SENSE -> actors.stream()
                    .filter(a -> a.getSense() == null || a.getSense().isEmpty())
                    .findFirst()
                    .ifPresent(a -> a.setSense(value));

        }

        return currentActor;
    }

    public void addElement(String element) {
        associations.put(element, null);
    }

    public void addActor(DreamActor actor) {
        this.actors.add(actor);
    }

    public DreamState getCurrentState() {
        return currentState;
    }

    public void changeState(DreamState newState) {
        this.currentState = newState;
    }

    public String associationsCollectForResult() {
        return associations.entrySet().stream()
                .map(e -> String.format("- *%s* - %s", e.getKey(), e.getValue()))
                .collect(Collectors.joining("\n"));
    }

    public String personsCollectForResult() {
        return actors.stream()
                .map(a -> String.format("- *%s* - %s", a.getPerson(), a.getCharacteristic()))
                .collect(Collectors.joining("\n"));
    }

    public String contextCollectForResult() {
        return actors.stream()
                .map(a -> String.format("- *%s* - %s", a.getCharacteristic(), a.getContext()))
                .collect(Collectors.joining("\n"));
    }

    public String senseCollectForResult() {
        return actors.stream()
                .map(a -> String.format("- *%s* - %s", a.getCharacteristic(), a.getSense()))
                .collect(Collectors.joining("\n"));
    }

}
