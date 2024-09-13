package com.example.dream_stream_bot.model;

public enum DreamState {
    NEW("New dream"),
    HISTORY("History of the dream"),
    ASSOCIATION("Associations related to the dream"),
    PERSONALITY("Personal characteristics involved"),
    CONTEXT("Context of the dream"),
    SENSE("Sensory experiences in the dream"),
    INTERPRETATION("Interpretation of the dream"),
    CANCEL("Cancel"),
    COMPLETE("complete");

    private final String description;

    DreamState(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

}
