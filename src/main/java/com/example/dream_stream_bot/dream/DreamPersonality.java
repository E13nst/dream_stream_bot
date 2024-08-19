package com.example.dream_stream_bot.dream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.util.*;

class DreamPersonality implements AnalyzerState {

    private static final Logger LOGGER = LoggerFactory.getLogger(DreamPersonality.class);

    private static final String MSG_DESCRIPTION = """
            Я выделил из твоей истории таких персонажей:

            %s

            Какая черта твоей личности ассоциируется с этим персонажем?
            Где в реальной жизни она проявляется? Что эта черта значит для тебя?

            Подберите ассоциации для слова:\040
            """;

    private static final String MSG_FAIL = "Я не смог выделить из твоей истории персонажей.";

    private static final String ACTORS_PROMPT = """
            Выбери из текста сновидения всех персонажей и действующих лиц вместе с их характеристиками.
            Список не должен включать меня самого. Не давай своих интерпретаций.
            Результат должен быть в виде списка без лишних комментариев
            в формате json, который будет содержать этих персонажей,
            например: ["красивая девушка","молчаливый незнакомец"]
            Текст для анализа:
            """;

    private static final String MSG_END = "Нажмите кнопку далее для продолжения";

    private final Deque<String> persons = new ArrayDeque<>();
    private String currentPerson;

    @Override
    public DreamStatus getState() {
        return DreamStatus.PERSONALITY;
    }

    @Override
    public void next(DreamAnalyzer analyzer) {
        analyzer.setState(new DreamInterpretation());
    }

    @Override
    public void prev(DreamAnalyzer analyzer) {
        analyzer.setState(new DreamAssociation());
    }

    @Override
    public List<SendMessage> init(DreamAnalyzer analyzer) {

        List<SendMessage> messages = new ArrayList<>();

        persons.addAll(analyzer.extractItemsAndSplit(ACTORS_PROMPT));

        String text;
        if (persons.isEmpty()) {
            LOGGER.warn("No elements found in dream analysis");
            text = MSG_FAIL;
        } else {
            text = String.format(MSG_DESCRIPTION, String.join("\n", persons));
        }

        SendMessage sendMessage = analyzer.newTelegramMessage(text);
        messages.add(sendMessage);
        return messages;
    }

    @Override
    public List<SendMessage> execute(DreamAnalyzer analyzer, String text) {

        List<SendMessage> messages = new ArrayList<>();

        if (text != null && !text.isBlank()) {
            analyzer.putAssociation(currentPerson, text);
            LOGGER.info("Association set for element {}: {}", currentPerson, text);
        } else {
            LOGGER.warn("Received blank message, skipping association");
        }

        currentPerson = persons.pollFirst();
        SendMessage sendMessage = analyzer.newTelegramMessage(Objects.requireNonNullElse(currentPerson, MSG_END));
        messages.add(sendMessage);
        return messages;
    }
}