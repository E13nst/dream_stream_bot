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
    public void next(DreamAnalyzer analyzer) {
        analyzer.setState(new DreamComplete());
    }

    @Override
    public void prev(DreamAnalyzer analyzer) {
        analyzer.setState(new DreamInterpretation());
    }

    @Override
    public List<SendMessage> execute(DreamAnalyzer analyzer, String text) {
        return null;
    }

    @Override
    public List<SendMessage> init(DreamAnalyzer analyzer) {
        return null;
    }
}