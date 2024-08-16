package com.example.dream_stream_bot.dream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;

class DreamAssociation implements AnalyzerState {

    private static final Logger LOGGER = LoggerFactory.getLogger(DreamAssociation.class);
    private static final String MSG_END = "Нажмите кнопку далее для продолжения";

    private static final String DESC_MSG = "Я выделил из твоей истории такие объекты:";
    private static final String NEXT_MSG = "На каждый объект тебе нужно придумать ассоциацию. Первое слово:\n\n";
    private static final String ERR_MSG = "Я не смог выделить из твоей истории объекты.";

    private static final String OBJECTS_PROMPT = "Выбери из текста сновидения все неодушевленные образы и предметы " +
            "вместе с их свойствами и характеристиками, которые можно использовать для анализа этого сновидения по Юнгу. " +
            "Не давай своих интерпретаций. Результат должен быть без лишних комментариев в виде списка в формате json, " +
            "который будет содержать эти предметы по такому образцу: \n" +
            "[\"красный спортивный автомобиль\",\"чистая холодная вода\"]\n" +
            "Список не должен включать персонажей и действующих лиц." +
            "Текст для анализа:\n";

    private String currentAssociation;

    @Override
    public DreamStatus getCurrentState() {
        return DreamStatus.ASSOCIATION;
    }

    @Override
    public void next(DreamAnalyzer dream) {
        dream.setState(new DreamActors());
    }

    @Override
    public void prev(DreamAnalyzer dream) {
        dream.setState(new DreamObjects());
    }

    @Override
    public String execute(DreamAnalyzer dream, String text) {

        if (currentAssociation != null) {
            dream.getObjects().put(currentAssociation, text);
            LOGGER.info("OBJECTS: {}", dream.getObjects());
        }

        StringBuilder stringBuilder = new StringBuilder();

        if (dream.getObjectList().isEmpty()) {

            String response = dream.extractItems(OBJECTS_PROMPT);
            dream.setObjectList(DreamAnalyzer.extractAndSplit(response));

            if (dream.getObjectList().isEmpty()) {
                stringBuilder.append(ERR_MSG);
            } else {
                stringBuilder
                        .append(DESC_MSG)
                        .append("\n\n")
                        .append(String.join("\n", dream.getObjectList()))
                        .append("\n\n")
                        .append(NEXT_MSG);
            }
        }

        Optional<String> firstEmptyKey = dream.getObjects().entrySet().stream()
                .filter(entry -> entry.getValue() == null || entry.getValue().isEmpty())
                .map(Map.Entry::getKey)
                .findFirst();

        currentAssociation = firstEmptyKey.orElse(MSG_END);
        stringBuilder.append(currentAssociation);
        return stringBuilder.toString();
    }

}