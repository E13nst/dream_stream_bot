package com.example.dream_stream_bot.dream;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.util.List;

class DreamStart implements AnalyzerState {

    @Override
    public DreamStatus getState() {
        return DreamStatus.NEW;
    }

    @Override
    public List<SendMessage> next(DreamAnalyzer analyzer) {
        analyzer.setState(new DreamHistory());
        return null;
    }

    @Override
    public void prev(DreamAnalyzer analyzer) {
    }

    @Override
    public List<SendMessage> processMessage(DreamAnalyzer dream, String text) {
        return null;
    }

}