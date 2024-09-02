package com.example.dream_stream_bot.dream;

import com.example.dream_stream_bot.model.InlineButtons;
import com.example.dream_stream_bot.model.InlineCommandKeyboard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

import java.util.ArrayList;
import java.util.List;

class DreamSense implements AnalyzerState {

    private static final Logger LOGGER = LoggerFactory.getLogger(DreamSense.class);

    private static final String MSG_SENSE_DESC = "Теперь мы вместе проанализируем твои черты личности";
    private static final String MSG_SENSE = "Что эти черты твоей личности для тебя значат: %s?";
    private static final String MSG_SENSE_END = "Мы определили такие значения твоих черт личности:";

    private DreamActor currentActor;

    private final InlineKeyboardMarkup keyboardMarkup = new InlineCommandKeyboard()
            .addKey("Продолжить \u2705", InlineButtons.NEXT.toString())
            .addKey("Отмена \u274C", InlineButtons.CANCEL.toString())
            .build();

    @Override
    public DreamStatus getState() {
        return DreamStatus.SENSE;
    }

    @Override
    public List<SendMessage> next(DreamAnalyzer analyzer) {
        analyzer.getDream().initActorsIterator();
        analyzer.setState(new DreamInterpretation());
        return null;
    }

    @Override
    public void prev(DreamAnalyzer analyzer) {
        analyzer.getDream().initActorsIterator();
        analyzer.setState(new DreamContext());
    }

    @Override
    public List<SendMessage> processMessage(DreamAnalyzer analyzer, String answer) {

        List<SendMessage> messages = new ArrayList<>();

        if (currentActor == null) {
            messages.add(analyzer.newTelegramMessage(MSG_SENSE_DESC));
        } else {
            currentActor.setSense(answer);
        }

        if (analyzer.getDream().hasActor()) {
            currentActor = analyzer.getDream().nextActor();
            messages.add(analyzer.newTelegramMessage(String.format(MSG_SENSE, currentActor.getCharacteristic())));
        } else {
            messages.add(analyzer.newTelegramMessage(MSG_SENSE_END));
            messages.add(analyzer.newTelegramMessage(analyzer.getDream().senseCollectForResult(), keyboardMarkup));
        }

        return messages;
    }

}