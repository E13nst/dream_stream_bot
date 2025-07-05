package com.example.dream_stream_bot.model;

import com.example.dream_stream_bot.model.dream.Dream;
import com.example.dream_stream_bot.model.dream.DreamActor;
import com.example.dream_stream_bot.model.dream.DreamState;
import io.qameta.allure.Step;
import net.datafaker.Faker;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Disabled("Тест отключен из-за проблем с переменными окружения")
class DreamTest {

    private final Faker faker = new Faker();

    @Test
    void getFirstUnassociatedDreamElement() {

        Dream dream = new Dream();
        List<String> elements = faker.<String>collection()
                .suppliers(() -> faker.animal().name())
                .minLen(2)
                .maxLen(4)
                .build().get();
        elements.forEach(dream::addElement);

        assertThat(dream.getFirstUnassociatedDreamElement()).isNotNull().isIn(elements);
        assertThat(dream.getAssociations().get(elements.get(0))).isNull();
    }

    @Test
    void setCurrentAssociation() {

        Dream dream = new Dream();
        List<String> elements = faker.<String>collection()
                .suppliers(() -> faker.animal().name()).len(5)
                .build().get();
        elements.forEach(dream::addElement);

        String element = dream.getFirstUnassociatedDreamElement();
        String association = faker.animal().scientificName();

        dream.setCurrentAssociation(association);

        assertThat(dream.getAssociations().get(element))
                .isNotNull()
                .isNotEmpty()
                .isEqualTo(association);

        assertThat(dream.getFirstUnassociatedDreamElement())
                .isNotNull()
                .isNotEmpty()
                .isNotEqualTo(element);
    }

    @Test
    void getFirstActorWithEmptyCharacteristic() {

        Dream dream = new Dream(DreamState.PERSONALITY);

        List<String> elements = faker.<String>collection()
                .suppliers(() -> faker.lordOfTheRings().character())
                .minLen(2)
                .maxLen(4)
                .build().get();
        elements.forEach(a -> dream.addActor(new DreamActor(a)));

        assertThat(dream.getNextActor().getPerson()).isNotNull().isEqualTo(elements.get(0));
        assertThat(dream.getActors().get(0).getCharacteristic()).isNull();
    }

    @Test
    void getFirstCharacteristicWithEmptyContext() {

        Dream dream = new Dream(DreamState.CONTEXT);
        List<DreamActor> elements = faker.<DreamActor>collection()
                .suppliers(() -> DreamActor.builder()
                        .person(faker.lordOfTheRings().character())
                        .characteristic(faker.color().name())
                        .build())
                .minLen(2)
                .maxLen(4)
                .build().get();

        elements.forEach(dream::addActor);

        assertThat(dream.getNextActor().getPerson()).isNotNull().isEqualTo(elements.get(0).getPerson());
        assertThat(dream.getActors().get(0).getContext()).isNull();
    }

    @Test
    void getFirstCharacteristicWithEmptySense() {

        Dream dream = new Dream(DreamState.SENSE);
        List<DreamActor> elements = faker.<DreamActor>collection()
                .suppliers(() -> DreamActor.builder()
                        .person(faker.lordOfTheRings().character())
                        .characteristic(faker.color().name())
                        .build())
                .minLen(2)
                .maxLen(4)
                .build().get();

        elements.forEach(dream::addActor);

        assertThat(dream.getNextActor().getPerson()).isNotNull().isEqualTo(elements.get(0).getPerson());
        assertThat(dream.getActors().get(0).getSense()).isNull();
    }

    @Test
    void shouldSetCurrentActorCharacteristic() {
        // Подготовка тестовых данных
        Dream dream = createDreamWithActors(5, DreamState.PERSONALITY);

        // Действие: находим первого актора без характористики
        DreamActor firstActorWithoutCharacteristic = findFirstActorWithoutParam(dream);

        // Генерация случайной характеристики
        String generatedValue = faker.lordOfTheRings().location();

        // Устанавливаем характеристику первому актору без неё
        setActorCharacteristic(firstActorWithoutCharacteristic, generatedValue);

        // Проверка: убеждаемся, что характеристика была установлена правильно
        verifyActorCharacteristic(firstActorWithoutCharacteristic, generatedValue);

        // Проверка: убеждаемся, что следующий актор без характеристики — это другой актор
        verifyNextActorWithoutParam(dream, firstActorWithoutCharacteristic);
    }

    @Test
    void shouldSetCurrentActorContext() {
        // Подготовка тестовых данных
        Dream dream = createDreamWithActors(5, DreamState.CONTEXT);

        // Действие: находим первого актора без контекста
        DreamActor firstActorWithoutContext = findFirstActorWithoutParam(dream);

        // Генерация случайного контекста
        String generatedValue = faker.lordOfTheRings().location();

        // Устанавливаем контекст первому актору без него
        setActorContext(firstActorWithoutContext, generatedValue);

        // Проверка: убеждаемся, что контекст был установлен правильно
        verifyActorContext(firstActorWithoutContext, generatedValue);

        // Проверка: убеждаемся, что следующий актор без контекста — это другой актор
        verifyNextActorWithoutParam(dream, firstActorWithoutContext);
    }

    @Step("Создаем сон с {actorCount} акторами")
    private Dream createDreamWithActors(int actorCount, DreamState state) {
        Dream dream = new Dream(state);
        List<String> actorNames = faker.<String>collection()
                .suppliers(() -> faker.lordOfTheRings().character())
                .len(actorCount)
                .build()
                .get();
        // Добавление акторов в сон
        actorNames.forEach(name -> dream.addActor(new DreamActor(name)));
        return dream;
    }

    @Step("Находим первого актора без характеристики")
    private DreamActor findFirstActorWithoutParam(Dream dream) {
        return dream.getNextActor();
    }

    @Step("Устанавливаем характеристику актору {actor}")
    private void setActorCharacteristic(DreamActor actor, String value) {
        actor.setCharacteristic(value);
    }

    @Step("Устанавливаем контекст актору {actor}")
    private void setActorContext(DreamActor actor, String value) {
        actor.setContext(value);
    }

    @Step("Проверяем, что характеристика актора установлена корректно")
    private void verifyActorCharacteristic(DreamActor actor, String expected) {
        assertThat(actor.getCharacteristic())
                .as("Проверяем, что характеристика была установлена")
                .isNotNull()
                .isNotEmpty()
                .isEqualTo(expected);
    }

    @Step("Проверяем, что контекст актора установлен корректно")
    private void verifyActorContext(DreamActor actor, String expected) {
        assertThat(actor.getContext())
                .as("Проверяем, что контекст был установлен")
                .isNotNull()
                .isNotEmpty()
                .isEqualTo(expected);
    }

    @Step("Проверяем, что следующий актор без характеристики — это другой актор")
    private void verifyNextActorWithoutParam(Dream dream, DreamActor previousActor) {
        DreamActor nextActorWithoutCharacteristic = dream.getNextActor();
        assertThat(nextActorWithoutCharacteristic.getPerson())
                .as("Проверяем, что следующий актор без характеристики не тот же самый")
                .isNotNull()
                .isNotEmpty()
                .isNotEqualTo(previousActor.getPerson());
    }

}