package com.example.dream_stream_bot.dream;

class DreamHistory implements AnalyzerState {

    private static final String HISTORY_DESCRIPTION =
            "Пожалуйста, опиши свой сон как можно подробнее. " +
                    "Ты можешь сделать это в нескольких сообщениях. ";

    @Override
    public DreamStatus getCurrentState() {
        return DreamStatus.HISTORY;
    }

    @Override
    public void next(DreamAnalyzer dream) {
        dream.setState(new DreamAssociation());
    }

    @Override
    public void prev(DreamAnalyzer dream) {
        dream.setState(new DreamNew());
    }

    @Override
    public String execute(DreamAnalyzer dream, String text) {
                dream.addHistory(text);

        return dream.getHistory().isBlank() ? HISTORY_DESCRIPTION : "";
    }

    @Override
    public String init(DreamAnalyzer dream) {
        return null;
    }
}