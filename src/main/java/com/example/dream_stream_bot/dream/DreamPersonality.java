package com.example.dream_stream_bot.dream;

import com.example.dream_stream_bot.model.InlineButtons;
import com.example.dream_stream_bot.model.InlineCommandKeyboard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.util.*;

class DreamPersonality implements AnalyzerState {

    private static final Logger LOGGER = LoggerFactory.getLogger(DreamPersonality.class);

    private static final String MSG_DESC_2 = "Какая черта твоей личности ассоциируется с этим персонажем? " +
            "Где в реальной жизни она проявляется? Что эта черта значит для тебя?";
    private static final String MSG_DESC_3 = "Подберите ассоциации для слова:";

    private static final String MSG_END = "У нас получились такие персонажи:";

    private static final String MSG_FAIL = "Я не смог выделить из твоей истории персонажей.";

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
    public List<SendMessage> run(DreamAnalyzer analyzer, String text) {

        List<SendMessage> messages = new ArrayList<>();

        var dream = analyzer.getDream();

        if (currentPerson == null || currentPerson.isBlank()) {
            if (dream.getActors().isEmpty()) {
                LOGGER.warn("No elements found in dream analysis");
                messages.add(analyzer.newTelegramMessage(MSG_FAIL));
            } else {
                messages.add(analyzer.newTelegramMessage(MSG_DESC_2));
                messages.add(analyzer.newTelegramMessage(MSG_DESC_3));
            }
        }

        if (text != null && !text.isBlank()) {
            dream.putPerson(currentPerson, text);
            LOGGER.info("Person set for element {}: {}", currentPerson, text);
        } else {
            LOGGER.warn("Received blank message, skipping person");
        }

        currentPerson = dream.pollFirstActor();

        if (currentPerson != null) {
            messages.add(analyzer.newTelegramMessage(currentPerson));
        }
        else {
            var keyboardMarkup = new InlineCommandKeyboard()
                    .addKey("Продолжить \u2705", InlineButtons.NEXT.toString())
                    .addKey("Отмена \u274C", InlineButtons.CANCEL.toString())
                    .build();

            messages.add(analyzer.newTelegramMessage(MSG_END));
            messages.add(analyzer.newTelegramMessage(dream.personsToString(), keyboardMarkup));
        }

        return messages;
    }

}