package com.example.dream_stream_bot.dream;

class DreamNew implements AnalyzerState {

    @Override
    public DreamStatus getCurrentState() {
        return DreamStatus.NEW;
    }

    @Override
    public void next(DreamAnalyzer dream) {
        dream.setState(new DreamHistory());
    }

    @Override
    public void prev(DreamAnalyzer dream) {
        dream.setState(new DreamObjects());
    }

    @Override
    public String execute(DreamAnalyzer dream, String text) {
        return "NEW RESULT";
    }
}