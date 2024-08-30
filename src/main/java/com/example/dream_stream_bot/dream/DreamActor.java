package com.example.dream_stream_bot.dream;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
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

    public String toStringForInterpretation() {
        return String.format("%s ассоциируется с моей чертой личности - %s. Это черта проявляется %s. " +
                "Она значит для меня - %s", person, characteristic, context, sense);
    }

}

