package com.example.dream_stream_bot.dream;

import com.example.dream_stream_bot.model.InlineButtons;
import com.example.dream_stream_bot.model.InlineCommandKeyboard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.util.*;

class DreamAssociation implements AnalyzerState {

    private static final Logger LOGGER = LoggerFactory.getLogger(DreamAssociation.class);

    private static final String MSG_DESC_1 = "Я выберу из твоего рассказа образы и предметы для подбора ассоциации.";
    private static final String MSG_DESC_2 = "Напиши, что каждый конкретный образ значит для тебя в контексте сна. " +
            "Если каждый образ вызывает несколько ассоциаций или воспоминаний — например, конкретного человека, " +
            "слова, фразы или ситуации — запиши все эти мысли.";
    private static final String MSG_DESC_3 = "Не переживай о правильности ассоциаций на этом этапе. Важно собрать разные варианты, " +
            "даже если они кажутся несвязанными. Наша цель — найти прямые ассоциации, которые возникают в связи с каждым образом.";
    private static final String MSG_DESC_4 = "Подбери ассоциации для слова: ";
    private static final String MSG_FAIL = "Я не смог выделить из твоей истории объекты.";
    private static final String MSG_END = "У нас получились такие ассоциации:";

    private String currentElement;

    @Override
    public DreamStatus getState() {
        return DreamStatus.ASSOCIATION;
    }

    @Override
    public List<SendMessage> next(DreamAnalyzer analyzer) {
        analyzer.setState(new DreamPersonality());
        return null;
    }

    @Override
    public void prev(DreamAnalyzer analyzer) {
        analyzer.setState(new DreamHistory());
    }

    @Override
    public List<SendMessage> run(DreamAnalyzer analyzer, String text) {

        List<SendMessage> messages = new ArrayList<>();

        var dream = analyzer.getDream();

        if (currentElement == null || currentElement.isBlank()) {
            if (dream.getElements().isEmpty()) {
                LOGGER.warn("No elements found in dream analysis");
                messages.add(analyzer.newTelegramMessage(MSG_FAIL));
            } else {
                messages.add(analyzer.newTelegramMessage(MSG_DESC_1));
                messages.add(analyzer.newTelegramMessage(MSG_DESC_2));
                messages.add(analyzer.newTelegramMessage(MSG_DESC_3));
                messages.add(analyzer.newTelegramMessage(MSG_DESC_4));
            }
        }

        if (text != null && !text.isBlank()) {
            dream.putAssociation(currentElement, text);
            LOGGER.info("Association set for element {}: {}", currentElement, text);
        } else {
            LOGGER.warn("Received blank message, skipping association");
        }

        currentElement = dream.pollFirstElement();

        if (currentElement != null) {
            messages.add(analyzer.newTelegramMessage(currentElement));
        }
        else {
            var keyboardMarkup = new InlineCommandKeyboard()
                    .addKey("Продолжить \u2705", InlineButtons.NEXT.toString())
                    .addKey("Отмена \u274C", InlineButtons.CANCEL.toString())
                    .build();

            messages.add(analyzer.newTelegramMessage(MSG_END));
            messages.add(analyzer.newTelegramMessage(dream.associationsCollectToString(), keyboardMarkup));
        }

        return messages;
    }

}