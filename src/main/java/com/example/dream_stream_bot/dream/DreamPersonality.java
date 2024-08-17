package com.example.dream_stream_bot.dream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;

class DreamPersonality implements AnalyzerState {

    private static final Logger LOGGER = LoggerFactory.getLogger(DreamPersonality.class);

    private static final String DESC_MSG = "Я выделил из твоей истории таких персонажей:";
    private static final String NEXT_MSG = "Какая черта твоей личности ассоциируется с этим персонажем? " +
            "Где в реальной жизни она проявляется? " +
            "Что эта черта значит для тебя?";

    private static final String ERR_MSG = "Я не смог выделить из твоей истории персонажей.";

    private static final String ACTORS_PROMPT = "Выбери из текста сновидения всех персонажей и действующих лиц " +
            "вместе с их характеристиками. Не давай своих интерпретаций. Результат должен быть в виде списка без лишних " +
            "комментариев в формате json, который будет содержать этих персонажей, например: " +
            "[\"красивая девушка\",\"молчаливый незнакомец\"]\n" +
            "Текст для анализа:\n";

    private static final String MSG_END = "Нажмите кнопку далее для продолжения";

    private String currentPerson;

    @Override
    public DreamStatus getCurrentState() {
        return DreamStatus.PERSONALITY;
    }

    @Override
    public void next(DreamAnalyzer dream) {
        dream.setState(new DreamInterpretation());
    }

    @Override
    public void prev(DreamAnalyzer dream) {
        dream.setState(new DreamAssociation());
    }

    @Override
    public String execute(DreamAnalyzer dream, String text) {

        if (currentPerson != null) {
            dream.getActors().put(currentPerson, text);
        }

        LOGGER.info("PERSONS: {}", dream.getActors());

        StringBuilder stringBuilder = new StringBuilder();

        if (dream.getActorsList().isEmpty()) {

            String response = dream.extractItems(ACTORS_PROMPT);
            dream.setActorsList(DreamAnalyzer.extractAndSplit(response));

            if (dream.getActorsList().isEmpty()) {
                stringBuilder.append(ERR_MSG);
            } else {
                stringBuilder
                        .append(DESC_MSG)
                        .append("\n\n")
                        .append(String.join("\n", dream.getActorsList()))
                        .append("\n\n")
                        .append(NEXT_MSG);
            }
        }

        Optional<String> firstEmptyKey = dream.getActors().entrySet().stream()
                .filter(entry -> entry.getValue() == null || entry.getValue().isEmpty())
                .map(Map.Entry::getKey)
                .findFirst();

        currentPerson = firstEmptyKey.orElse(MSG_END);
        return stringBuilder.toString();
    }

    @Override
    public String init(DreamAnalyzer dream) {
        return null;
    }
}