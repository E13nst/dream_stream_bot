package com.example.dream_stream_bot.dream;

import com.example.dream_stream_bot.model.InlineButtons;
import com.example.dream_stream_bot.model.InlineCommandKeyboard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

import java.util.*;

class DreamContext implements AnalyzerState {

    private static final Logger LOGGER = LoggerFactory.getLogger(DreamContext.class);

    private static final String MSG_CONTEXT_DESC = "Теперь мы вместе проанализируем твои черты личности";
    private static final String MSG_CONTEXT = "Где в твоей жизни проявляются эти черты: %s?";
    private static final String MSG_CONTEXT_END = "Твои черты личности проявляются:";

    private DreamActor currentActor;

    private final InlineKeyboardMarkup keyboardMarkup = new InlineCommandKeyboard()
            .addKey("Продолжить \u2705", InlineButtons.NEXT.toString())
            .addKey("Отмена \u274C", InlineButtons.CANCEL.toString())
            .build();

    @Override
    public DreamStatus getState() {
        return DreamStatus.CONTEXT;
    }

    @Override
    public List<SendMessage> next(DreamAnalyzer analyzer) {
        analyzer.getDream().initActorsIterator();
        analyzer.setState(new DreamSense());
        return null;
    }

    @Override
    public void prev(DreamAnalyzer analyzer) {
        analyzer.getDream().initActorsIterator();
        analyzer.setState(new DreamPersonality());
    }

    @Override
    public List<SendMessage> processMessage(DreamAnalyzer analyzer, String answer) {

        List<SendMessage> messages = new ArrayList<>();

        if (currentActor == null) {
            messages.add(analyzer.newTelegramMessage(MSG_CONTEXT_DESC));
        } else {
            currentActor.setContext(answer);
        }

        if (analyzer.getDream().hasActor()) {
            currentActor = analyzer.getDream().nextActor();
            messages.add(analyzer.newTelegramMessage(String.format(MSG_CONTEXT, currentActor.getCharacteristic())));
        } else {
            messages.add(analyzer.newTelegramMessage(MSG_CONTEXT_END));
            messages.add(analyzer.newTelegramMessage(analyzer.getDream().contextCollectForResult(), keyboardMarkup));
        }

        return messages;
    }

}