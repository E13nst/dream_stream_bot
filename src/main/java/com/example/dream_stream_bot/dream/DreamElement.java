package com.example.dream_stream_bot.dream;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
public class DreamElement {
    @Getter
    @Setter
    String name;
    @Getter
    @Setter
    String association;

    public DreamElement(String name) {
        this.name = name;
    }
}

