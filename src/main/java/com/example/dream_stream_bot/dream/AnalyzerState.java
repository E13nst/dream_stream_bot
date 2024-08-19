package com.example.dream_stream_bot.dream;

interface AnalyzerState {
    DreamStatus getState();
    void next(DreamAnalyzer dream);
    void prev(DreamAnalyzer dream);
    String execute(DreamAnalyzer dream, String text);
    String init(DreamAnalyzer dream);
//    String getResult(DreamAnalyzer dream);

}