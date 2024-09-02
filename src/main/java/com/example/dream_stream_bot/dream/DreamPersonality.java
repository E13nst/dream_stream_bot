package com.example.dream_stream_bot.dream;

import com.example.dream_stream_bot.model.InlineButtons;
import com.example.dream_stream_bot.model.InlineCommandKeyboard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

import java.util.*;

class DreamPersonality implements AnalyzerState {

    private static final Logger LOGGER = LoggerFactory.getLogger(DreamPersonality.class);

    private static final String MSG_DESC_1 = "Теперь мы будем работать с персонажами сновидения.";
    private static final String MSG_PERSON = "Какая черта твоей личности ассоциируется с этим персонажем?";
    private static final String MSG_END = "У нас получились такие персонажи:";
    private static final String MSG_FAIL = "Я не смог выделить из твоей истории персонажей.";

    private DreamActor currentActor;

    InlineKeyboardMarkup keyboardMarkup = new InlineCommandKeyboard()
            .addKey("Продолжить \u2705", InlineButtons.NEXT.toString())
            .addKey("Отмена \u274C", InlineButtons.CANCEL.toString())
            .build();

    @Override
    public DreamStatus getState() {
        return DreamStatus.PERSONALITY;
    }

    @Override
    public List<SendMessage> next(DreamAnalyzer analyzer) {
        analyzer.getDream().initActorsIterator();
        analyzer.setState(new DreamContext());
        return null;
    }

    @Override
    public void prev(DreamAnalyzer analyzer) {
        analyzer.getDream().initActorsIterator();
        analyzer.setState(new DreamAssociation(analyzer.getDream()));
    }

    @Override
    public List<SendMessage> processMessage(DreamAnalyzer analyzer, String answer) {

        List<SendMessage> messages = new ArrayList<>();

        if (currentActor == null) {
            messages.add(analyzer.newTelegramMessage(MSG_DESC_1));
        } else {
            currentActor.setCharacteristic(answer);
        }

        if (analyzer.getDream().hasActor()) {
            currentActor = analyzer.getDream().nextActor();
            messages.add(analyzer.newTelegramMessage(MSG_PERSON));
            messages.add(analyzer.newTelegramMessage(currentActor.getName()));
        } else {
            messages.add(analyzer.newTelegramMessage(MSG_END));
            messages.add(analyzer.newTelegramMessage(analyzer.getDream().personsCollectForResult(), keyboardMarkup));
        }

        return messages;
    }

}