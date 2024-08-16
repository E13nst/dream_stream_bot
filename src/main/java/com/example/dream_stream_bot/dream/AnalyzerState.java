package com.example.dream_stream_bot.dream;

interface AnalyzerState {
    void next(DreamAnalyzer dream);
    void prev(DreamAnalyzer dream);
    String execute(DreamAnalyzer dream, String text);
    DreamStatus getCurrentState();
//    String getDescription(DreamAnalyzer dream);
//    String getResult(DreamAnalyzer dream);

}