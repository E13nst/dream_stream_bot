package com.example.dream_stream_bot.dream;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.util.ArrayList;
import java.util.List;

class DreamInterpretation implements AnalyzerState {

    private static final String HISTORY_DESCRIPTION =
            "Интерпритация";

    @Override
    public DreamStatus getState() {
        return DreamStatus.INTERPRETATION;
    }

    @Override
    public void next(DreamAnalyzer analyzer) {
        analyzer.setState(new DreamPersonality());
    }

    @Override
    public void prev(DreamAnalyzer analyzer) {
        analyzer.setState(new DreamPersonality());
    }

    @Override
    public List<SendMessage> run(DreamAnalyzer analyzer, String text) {
        List<SendMessage> messages = new ArrayList<>();
        String response = AiTextProcessor.interpretDream(
                analyzer.getOpenaiChat(),
                analyzer.getUserName(),
                analyzer.getDream()
        );
        messages.add(analyzer.newTelegramMessage(response));
        return messages;
    }
}