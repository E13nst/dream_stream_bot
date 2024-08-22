package com.example.dream_stream_bot.dream;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.util.ArrayList;
import java.util.List;

class DreamComplete implements AnalyzerState {

    @Override
    public DreamStatus getState() {
        return DreamStatus.COMPLETE;
    }

    @Override
    public List<SendMessage> next(DreamAnalyzer analyzer) {
        analyzer.setState(new DreamComplete());
        return null;
    }

    @Override
    public void prev(DreamAnalyzer analyzer) {
        analyzer.setState(new DreamInterpretation());
    }

    @Override
    public List<SendMessage> run(DreamAnalyzer analyzer, String text) {
        return new ArrayList<>();
    }
}