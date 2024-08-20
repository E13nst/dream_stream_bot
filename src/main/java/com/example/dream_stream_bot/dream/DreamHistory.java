package com.example.dream_stream_bot.dream;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.util.ArrayList;
import java.util.List;

class DreamHistory implements AnalyzerState {

    private static final String HISTORY_DESCRIPTION =
            "Пожалуйста, опиши свой сон как можно подробнее. " +
                    "Ты можешь сделать это в нескольких сообщениях. ";

    @Override
    public DreamStatus getState() {
        return DreamStatus.HISTORY;
    }

    @Override
    public void next(DreamAnalyzer analyzer) {
        analyzer.setState(new DreamAssociation());
    }

    @Override
    public void prev(DreamAnalyzer analyzer) {
        analyzer.setState(new DreamNew());
    }

    @Override
    public List<SendMessage> init(DreamAnalyzer analyzer) {
        List<SendMessage> messages = new ArrayList<>();
        messages.add(analyzer.newTelegramMessage(HISTORY_DESCRIPTION));
        return messages;
    }

    @Override
    public List<SendMessage> execute(DreamAnalyzer analyzer, String text) {
        analyzer.getDream().addHistory(text);
        return new ArrayList<>();
    }
}