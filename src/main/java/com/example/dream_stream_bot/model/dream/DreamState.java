package com.example.dream_stream_bot.model.dream;

import java.util.Arrays;

public enum DreamState {
    //    NEW("New dream"),
    HISTORY("History of the dream"),
    ASSOCIATION("Associations related to the dream"),
    PERSONALITY("Personal characteristics involved"),
    CONTEXT("Context of the dream"),
    SENSE("Sensory experiences in the dream"),
    INTERPRETATION("Interpretation of the dream"),
    COMPLETE("complete"),
    CANCEL("Cancel");

    private final String description;

    DreamState(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public static DreamState fromString(String value) {
        return Arrays.stream(DreamState.values())
                .filter(state -> state.name().equals(value))
                .findFirst()
                .orElse(null);
    }
}
