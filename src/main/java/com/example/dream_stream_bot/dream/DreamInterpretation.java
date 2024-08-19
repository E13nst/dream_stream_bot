package com.example.dream_stream_bot.dream;

class DreamInterpretation implements AnalyzerState {

    private static final String HISTORY_DESCRIPTION =
            "Интерпритация";

    @Override
    public DreamStatus getState() {
        return DreamStatus.INTERPRETATION;
    }

    @Override
    public void next(DreamAnalyzer dream) {
        dream.setState(new DreamPersonality());
    }

    @Override
    public void prev(DreamAnalyzer dream) {
        dream.setState(new DreamPersonality());
    }

    @Override
    public String execute(DreamAnalyzer dream, String text) {
        return dream.analyze();
    }

    @Override
    public String init(DreamAnalyzer dream) {
        return null;
    }
}