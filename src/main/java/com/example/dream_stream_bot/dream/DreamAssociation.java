package com.example.dream_stream_bot.dream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

class DreamAssociation implements AnalyzerState {

    private static final Logger LOGGER = LoggerFactory.getLogger(DreamAssociation.class);
    private static final String MSG_END = "Нажмите кнопку далее для продолжения";

    private static final String MSG_DESCRIPTION = "Я выбрал из твоего рассказа следующие образы и предметы для ассоциации:\n" +
            "\n" +
            "%s\n" +
            "\n" +
            "Теперь напишите, что каждый из этих образов означает для вас в контексте сна.\n" +
            "\n" +
            "Если каждый образ вызывает у вас несколько ассоциаций или воспоминаний — например, конкретного человека, " +
            "слова, фразы или ситуации — запишите все эти мысли.\n" +
            "\n" +
            "Не переживайте о правильности ассоциаций на этом этапе; важно собрать разные варианты, " +
            "даже если они кажутся несвязанными. " +
            "Ваша цель — найти прямые ассоциации, которые возникают в связи с каждым образом.\n" +
            "\n" +
            "Подберите ассоциации для слова: ";

    private static final String MSG_FAIL = "Я не смог выделить из твоей истории объекты.";

    private static final String OBJECTS_PROMPT = "Выбери из текста сновидения все неодушевленные образы и предметы " +
            "вместе с их свойствами и характеристиками, которые можно использовать для анализа этого сновидения по Юнгу. " +
            "Не давай своих интерпретаций. Результат должен быть без лишних комментариев в виде списка в формате json, " +
            "который будет содержать эти предметы по такому образцу: \n" +
            "[\"красный спортивный автомобиль\",\"чистая холодная вода\"]\n" +
            "Список не должен включать персонажей и действующих лиц." +
            "Текст для анализа:\n";

    private final Deque<String> elements = new ArrayDeque<>();
    private String currentElement;

    @Override
    public DreamStatus getCurrentState() {
        return DreamStatus.ASSOCIATION;
    }

    @Override
    public void next(DreamAnalyzer dream) {
        dream.setState(new DreamPersonality());
    }

    @Override
    public void prev(DreamAnalyzer dream) {
        dream.setState(new DreamHistory());
    }

    @Override
    public String init(DreamAnalyzer analyzer) {

        String response = analyzer.extractItems(OBJECTS_PROMPT);
        elements.addAll(DreamAnalyzer.extractAndSplit(response));

        if (elements.isEmpty()) {
            return MSG_FAIL;
        } else {
            return String.format(MSG_DESCRIPTION, String.join("\n", elements));
        }
    }

    @Override
    public String execute(DreamAnalyzer analyzer, String message) {

        if (message != null && !message.isBlank()) {
            analyzer.setAssociation(currentElement, message);
        }

        currentElement = elements.pollFirst();
        return currentElement != null ? currentElement : MSG_END;
    }
}