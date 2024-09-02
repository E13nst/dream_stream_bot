package com.example.dream_stream_bot.dream;

import com.example.dream_stream_bot.common.Steps;
import com.example.dream_stream_bot.config.BotConfig;
import net.datafaker.Faker;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class DreamAnalyzerTest {

    @Autowired
    private BotConfig botConfig;
    @Autowired
    private Steps steps;

    private final Faker faker = new Faker();

    @Test
    void shouldReturnCorrectMessagesForSetSenses() {

        DreamActor[] actors = new DreamActor[]{
        DreamActor.builder()
                .characteristic(faker.lebowski().character())
                .build(),
        DreamActor.builder()
                .characteristic(faker.lebowski().character())
                .build()
        };

        Dream dream = new Dream();
        dream.addAllActors(Arrays.asList(actors));

        DreamAnalyzer analyzer = DreamAnalyzer.builder().dream(dream).build();
        analyzer.setState(new DreamAnalyzer.StateSense());

        String sense0 = faker.lebowski().quote();
        String sense1 = faker.lebowski().quote();

        List<SendMessage> messages = analyzer.processMessage("");
        assertThat(messages).hasSize(2);
        assertThat(messages).anyMatch(e -> e.getText().contains(actors[0].getCharacteristic()));
        assertThat(messages.getFirst().getText()).contains("черты личности");
        assertThat(messages.getLast().getText()).contains("значат");
        assertThat(messages.getLast().getText()).contains(actors[0].getCharacteristic());

        messages = analyzer.processMessage(sense0);
        assertThat(messages).hasSize(1);
        assertThat(messages.getLast().getText()).contains(actors[1].getCharacteristic());
        assertThat(analyzer.getDream().getActors().get(0).getSense()).isEqualTo(sense0);

        messages = analyzer.processMessage(sense1);
        assertThat(messages).hasSize(2);
        assertThat(messages.getFirst().getText()).contains("значения");
        assertThat(messages.getLast().getText())
                .contains(actors[0].getCharacteristic())
                .contains(sense0)
                .contains(actors[1].getCharacteristic())
                .contains(sense1);
    }

}