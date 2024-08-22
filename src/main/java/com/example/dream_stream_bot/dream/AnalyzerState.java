package com.example.dream_stream_bot.dream;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.util.List;
import java.util.Optional;

interface AnalyzerState {
    DreamStatus getState();
    List<SendMessage> next(DreamAnalyzer dream);
    void prev(DreamAnalyzer dream);
    List<SendMessage> run(DreamAnalyzer dream, String text);
}