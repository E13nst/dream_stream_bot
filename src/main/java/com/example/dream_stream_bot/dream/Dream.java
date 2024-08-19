package com.example.dream_stream_bot.dream;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

public class Dream {
    private final StringBuilder history = new StringBuilder();
    @Getter
    private final Map<String, String> associations = new HashMap<>();
    @Getter
    private final Map<String, String> persons = new HashMap<>();

    public void addHistory(String text) {
        history.append(text).append("\n");
    }

    public String getHistoryStr() {
        return history.toString();
    }

    public void putAssociation(String key, String value) {
        associations.put(key, value);
    }
}
