package com.example.dream_stream_bot.model;

import java.util.Arrays;

public enum DreamCommand {
    CREATE,
    WRITE_HISTORY,
    FIND_ASSOCIATIONS,
    FIND_ACTORS,
    SET_CONTEXT,
    SET_SENSE,
    INTERPRETATION,
    COMPLETE,
    CANCEL,
    NOP;

    public static DreamCommand fromString(String value) {
        return Arrays.stream(DreamCommand.values())
                .filter(state -> state.name().equals(value))
                .findFirst()
                .orElse(DreamCommand.NOP);
    }
}
