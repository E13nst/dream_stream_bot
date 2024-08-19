package com.example.dream_stream_bot.dream;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.util.List;

interface AnalyzerState {
    DreamStatus getState();
    void next(DreamAnalyzer dream);
    void prev(DreamAnalyzer dream);
    List<SendMessage> execute(DreamAnalyzer dream, String text);
    List<SendMessage> init(DreamAnalyzer dream);
}