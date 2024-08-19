package com.example.dream_stream_bot.dream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.util.*;

class DreamAssociation implements AnalyzerState {

    private static final Logger LOGGER = LoggerFactory.getLogger(DreamAssociation.class);
    private static final String MSG_END = "Нажмите кнопку далее для продолжения";

    private static final String MSG_DESCRIPTION = """
            Я выбрал из твоего рассказа следующие образы и предметы для ассоциации:

            %s

            Теперь напишите, что каждый из этих образов означает для вас в контексте сна.
            Если каждый образ вызывает у вас несколько ассоциаций или воспоминаний — например, конкретного человека,
            слова, фразы или ситуации — запишите все эти мысли.

            Не переживайте о правильности ассоциаций на этом этапе; важно собрать разные варианты, даже если они кажутся несвязанными.
            Ваша цель — найти прямые ассоциации, которые возникают в связи с каждым образом.

            Подберите ассоциации для слова:\040
            """;

    private static final String MSG_FAIL = "Я не смог выделить из твоей истории объекты.";

    private static final String OBJECTS_PROMPT = """
            Выбери из текста сновидения все неодушевленные образы и предметы вместе с их свойствами и характеристиками, 
            которые можно использовать для анализа этого сновидения по Юнгу. Не давай своих интерпретаций. 
            Результат должен быть без лишних комментариев в виде списка в формате json, который будет содержать 
            эти предметы по такому образцу:\s
            ["красный спортивный автомобиль","чистая холодная вода"]
            Список не должен включать персонажей и действующих лиц.Текст для анализа:
            """;

    private final Deque<String> elements = new ArrayDeque<>();
    private String currentElement;

    @Override
    public DreamStatus getState() {
        return DreamStatus.ASSOCIATION;
    }

    @Override
    public void next(DreamAnalyzer analyzer) {
        analyzer.setState(new DreamPersonality());
    }

    @Override
    public void prev(DreamAnalyzer analyzer) {
        analyzer.setState(new DreamHistory());
    }

    @Override
    public List<SendMessage> init(DreamAnalyzer analyzer) {

        List<SendMessage> messages = new ArrayList<>();

        elements.addAll(analyzer.extractItemsAndSplit(OBJECTS_PROMPT));

        String text;
        if (elements.isEmpty()) {
            LOGGER.warn("No elements found in dream analysis");
            text = MSG_FAIL;
        } else {
            text = String.format(MSG_DESCRIPTION, String.join("\n", elements));
        }

        SendMessage sendMessage = analyzer.newTelegramMessage(text);
        messages.add(sendMessage);
        return messages;
    }

    @Override
    public List<SendMessage> execute(DreamAnalyzer analyzer, String text) {

        List<SendMessage> messages = new ArrayList<>();

        if (text != null && !text.isBlank()) {
            analyzer.getDream().putAssociation(currentElement, text);
            LOGGER.info("Association set for element {}: {}", currentElement, text);
        } else {
            LOGGER.warn("Received blank message, skipping association");
        }

        currentElement = elements.pollFirst();

        SendMessage sendMessage = analyzer.newTelegramMessage(Objects.requireNonNullElse(currentElement, MSG_END));
        messages.add(sendMessage);
        return messages;
    }
}