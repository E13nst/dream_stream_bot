package com.example.dream_stream_bot.model.dream;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Builder
public class DreamActor {
    @Getter
    @Setter
    String person;
    @Getter
    @Setter
    String characteristic;
    @Getter
    @Setter
    String context;
    @Getter
    @Setter
    String sense;

    public DreamActor(String person) {
        this.person = person;
    }

}

