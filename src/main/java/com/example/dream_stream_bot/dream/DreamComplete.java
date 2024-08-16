package com.example.dream_stream_bot.dream;

class DreamComplete implements AnalyzerState {

    @Override
    public DreamStatus getCurrentState() {
        return DreamStatus.COMPLETE;
    }

    @Override
    public void next(DreamAnalyzer dream) {
        dream.setState(new DreamComplete());
    }

    @Override
    public void prev(DreamAnalyzer dream) {
        dream.setState(new DreamInterpretation());
    }

    @Override
    public String execute(DreamAnalyzer dream, String text) {
        if (!text.isBlank())
            dream.addHistory(text);

        return "END";
    }
}