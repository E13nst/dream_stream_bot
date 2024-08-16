package com.example.dream_stream_bot.dream;

class DreamActors implements AnalyzerState {

    private static final String DESC_MSG = "Я выделил из твоей истории таких персонажей:";
    private static final String NEXT_MSG = "Какая черта твоей личности ассоциируется с этим персонажем? " +
            "Где в реальной жизни она проявляется? " +
            "Что эта черта значит для тебя?";

    private static final String ERR_MSG = "Я не смог выделить из твоей истории персонажей.";

    private static final String ACTORS_PROMPT = "Выбери из текста сновидения всех персонажей и действующих лиц " +
            "вместе с их характеристиками. Не давай своих интерпретаций. Результат должен быть в виде списка без лишних " +
            "комментариев в формате json, который будет содержать этих персонажей, например: " +
            "[\"красивая девушка\",\"молчаливый незнакомец\"]\n" +
            "Текст для анализа:\n";

    @Override
    public DreamStatus getCurrentState() {
        return DreamStatus.ACTORS;
    }

    @Override
    public void next(DreamAnalyzer dream) {
        dream.setState(new DreamPersonality());
    }

    @Override
    public void prev(DreamAnalyzer dream) {
        dream.setState(new DreamAssociation());
    }

    @Override
    public String execute(DreamAnalyzer dream, String text) {
        String response = dream.extractItems(ACTORS_PROMPT);
        dream.setActorsList(DreamAnalyzer.extractAndSplit(response));

        return dream.getActorsList().isEmpty() ?
                ERR_MSG :
                new StringBuilder()
                        .append(DESC_MSG)
                        .append("\n\n")
                        .append(String.join("\n", dream.getActorsList()))
                        .append("\n\n")
                        .append(NEXT_MSG)
                        .toString();
    }
}