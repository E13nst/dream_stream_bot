package com.example.dream_stream_bot.dream;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Builder
public class DreamActor {
    @Getter
    @Setter
    String name;
    @Getter
    @Setter
    String characteristic;
    @Getter
    @Setter
    String context;
    @Getter
    @Setter
    String sense;

    public DreamActor(String name) {
        this.name = name;
    }

}

