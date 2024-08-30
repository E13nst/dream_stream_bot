package com.example.dream_stream_bot.dream;

import com.example.dream_stream_bot.model.InlineButtons;
import com.example.dream_stream_bot.model.InlineCommandKeyboard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.util.*;

class DreamPersonality implements AnalyzerState {

    private static final Logger LOGGER = LoggerFactory.getLogger(DreamPersonality.class);

    private static final String MSG_DESC_1 = "Теперь мы будем работать с персонажами сновидения.";
    private static final String MSG_DESC_2 = "Какая черта твоей личности ассоциируется с этим персонажем? " +
            "Где в реальной жизни она проявляется? Что эта черта значит для тебя?";
    private static final String MSG_END = "У нас получились такие персонажи:";
    private static final String MSG_FAIL = "Я не смог выделить из твоей истории персонажей.";

    private final Iterator<DreamActor> iterator;
    private DreamActor currentDreamActor;

    DreamPersonality(DreamAnalyzer analyzer) {
        this.iterator = analyzer.getDream().getActors().iterator();
    }

    @Override
    public DreamStatus getState() {
        return DreamStatus.PERSONALITY;
    }

    @Override
    public List<SendMessage> next(DreamAnalyzer analyzer) {
        analyzer.setState(new DreamInterpretation());
        return null;
    }

    @Override
    public void prev(DreamAnalyzer analyzer) {
        analyzer.setState(new DreamAssociation(analyzer));
    }

    @Override
    public List<SendMessage> run(DreamAnalyzer analyzer, String characteristic) {

        List<SendMessage> messages = new ArrayList<>();
        var dream = analyzer.getDream();

        if (currentDreamActor == null || currentDreamActor.getPerson().isBlank()) {
            if (!iterator.hasNext()) {
                LOGGER.warn("No elements found in dream analysis");
                messages.add(analyzer.newTelegramMessage(MSG_FAIL));
            } else {
                messages.add(analyzer.newTelegramMessage(MSG_DESC_1));
                messages.add(analyzer.newTelegramMessage(MSG_DESC_2));
            }
        }

        if (characteristic != null && !characteristic.isBlank()) {
            currentDreamActor.setCharacteristic(characteristic);
            LOGGER.info("Person set for element {}: {}", currentDreamActor, characteristic);
        } else {
            LOGGER.warn("Received blank message, skipping person");
        }

        if (iterator.hasNext()) {
            currentDreamActor = iterator.next();
            messages.add(analyzer.newTelegramMessage(currentDreamActor.getPerson()));
        }
        else {
            var keyboardMarkup = new InlineCommandKeyboard()
                    .addKey("Продолжить \u2705", InlineButtons.NEXT.toString())
                    .addKey("Отмена \u274C", InlineButtons.CANCEL.toString())
                    .build();

            messages.add(analyzer.newTelegramMessage(MSG_END));
            messages.add(analyzer.newTelegramMessage(dream.personsCollectToString(), keyboardMarkup));
        }

        return messages;
    }

}