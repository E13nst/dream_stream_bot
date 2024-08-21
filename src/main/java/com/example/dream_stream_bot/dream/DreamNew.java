package com.example.dream_stream_bot.dream;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.util.List;

class DreamNew implements AnalyzerState {

    @Override
    public DreamStatus getState() {
        return DreamStatus.NEW;
    }

    @Override
    public void next(DreamAnalyzer dream) {
        dream.setState(new DreamHistory());
    }

    @Override
    public void prev(DreamAnalyzer dream) {
        dream.setState(new DreamAssociation());
    }

    @Override
    public List<SendMessage> run(DreamAnalyzer dream, String text) {
        return null;
    }

}