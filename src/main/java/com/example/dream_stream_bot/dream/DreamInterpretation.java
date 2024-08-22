package com.example.dream_stream_bot.dream;

import com.example.dream_stream_bot.model.InlineButtons;
import com.example.dream_stream_bot.model.InlineCommandKeyboard;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.util.ArrayList;
import java.util.List;

class DreamInterpretation implements AnalyzerState {

    @Override
    public DreamStatus getState() {
        return DreamStatus.INTERPRETATION;
    }

    @Override
    public List<SendMessage> next(DreamAnalyzer analyzer) {
        return null;
    }

    @Override
    public void prev(DreamAnalyzer analyzer) {
        analyzer.setState(new DreamPersonality());
    }

    @Override
    public List<SendMessage> run(DreamAnalyzer analyzer, String text) {

        String response = AiTextProcessor.interpretDream(
                analyzer.getOpenaiChat(),
                analyzer.getUserName(),
                analyzer.getDream()
        );

        analyzer.setState(new DreamComplete());

        List<SendMessage> messages = new ArrayList<>();
        var keyboard = new InlineCommandKeyboard()
                .addKey("Завершить \u274C", InlineButtons.CANCEL.toString())
                .build();
        messages.add(analyzer.newTelegramMessage(response, keyboard));
        return messages;
    }
}