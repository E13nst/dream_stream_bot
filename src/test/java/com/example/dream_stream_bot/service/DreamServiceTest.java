package com.example.dream_stream_bot.service;

import com.example.dream_stream_bot.config.BotConfig;
import com.example.dream_stream_bot.model.DreamActor;
import net.datafaker.Faker;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest
@Disabled("Тест отключен из-за проблем с переменными окружения")
class DreamServiceTest {

    String text = "Приснилось ему его детство, еще в их городке. Он лет семи и гуляет в праздничный день, под вечер, " +
            "с своим отцом за городом. Время серенькое, день удушливый, местность совершенно такая же, как уцелела " +
            "в его памяти: даже в памяти его она гораздо более изгладилась, чем представлялась теперь во сне. " +
            "Городок стоит открыто, как на ладони, кругом ни ветлы; где-то очень далеко, на самом краю неба, чернеется лесок. " +
            "В нескольких шагах от последнего городского огорода стоит кабак, большой кабак, всегда производивший " +
            "на него неприятнейшее впечатление и даже страх, когда он проходил мимо его, гуляя с отцом. Там всегда " +
            "была такая толпа, так орали, хохотали, ругались, так безобразно и сипло пели и так часто дрались; " +
            "кругом кабака шлялись всегда такие пьяные и страшные рожи... ";

    @Autowired
    private DreamService dreamService;
    @Autowired
    private BotConfig botConfig;
    @Autowired
    private AIService aiService;

    private final Faker faker = new Faker();

    @Test
    void testCompletionIntegration() {
        assertThat(aiService.completion(11, "test", "test")).isNotEmpty();
    }

    @Test
    void shouldAddDreamTextSuccessfully() {

        long defaultUserId = faker.number().randomNumber();

        List<String> quotes = faker.<String>collection()
                .suppliers(() -> faker.lebowski().quote())
                .minLen(5)
                .maxLen(10)
                .build().get();

        quotes.forEach(t -> dreamService.addDreamText(defaultUserId, t));
        assertThat(dreamService.getDreamText(defaultUserId)).isEqualTo(String.join("\n", quotes));
    }

    @Test
    void addDreamTextForSomeUsers() {

        long userId1 = faker.number().randomNumber();
        long userId2 = faker.number().randomNumber();

        List<String> quotes1 = faker.<String>collection()
                .suppliers(() -> faker.lebowski().quote())
                .minLen(5)
                .maxLen(10)
                .build().get();

        List<String> quotes2 = faker.<String>collection()
                .suppliers(() -> faker.lebowski().quote())
                .minLen(5)
                .maxLen(10)
                .build().get();

        quotes1.forEach(t -> dreamService.addDreamText(userId1, t));
        assertThat(dreamService.getDreamText(userId1)).isEqualTo(String.join("\n", quotes1));

        quotes2.forEach(t -> dreamService.addDreamText(userId2, t));
        assertThat(dreamService.getDreamText(userId2)).isEqualTo(String.join("\n", quotes2));
    }

    @Test
    void findAssociations() {

        long userId = faker.number().randomNumber();
        dreamService.create(userId);

        dreamService.addDreamText(userId, text);
        String result = dreamService.findDreamElements(userId);

        List<String> associationNames = new ArrayList<>(dreamService.getUserDream(userId).getAssociations().keySet());
        assertThat(associationNames).contains("лесок", "кабак", "толпа");
    }

    @Test
    void findDreamActors() {

        long userId = faker.number().randomNumber();
        dreamService.create(userId);

        dreamService.addDreamText(userId, text);
        String stepDescription = dreamService.findDreamActors(userId);
        assertThat(stepDescription).isNotNull().isNotEmpty();

        List<String> actors = dreamService.getUserDream(userId).getActors().stream().map(DreamActor::getPerson).toList();
        assertThat(actors).contains("отец", "толпа");
    }

}