package com.example.dream_stream_bot.dream;

class DreamObjects implements AnalyzerState {

    private static final String DESC_MSG = "Я выделил из твоей истории такие объекты:";
    private static final String NEXT_MSG = "На каждый объект тебе нужно придумать ассоциацию.";
    private static final String ERR_MSG = "Я не смог выделить из твоей истории объекты.";

    private static final String OBJECTS_PROMPT = "Выбери из текста сновидения все неодушевленные образы и предметы " +
            "вместе с их свойствами и характеристиками, которые можно использовать для анализа этого сновидения по Юнгу. " +
            "Не давай своих интерпретаций. Результат должен быть в виде списка без лишних комментариев в формате json, " +
            "который будет содержать эти предметы, например: \n" +
            "[\"красный спортивный автомобиль\",\"чистая холодная вода\"]\n" +
            "Список не должен включать персонажей и действующих лиц." +
            "Текст для анализа:\n";

    @Override
    public DreamStatus getCurrentState() {
        return DreamStatus.OBJECTS;
    }

    @Override
    public void next(DreamAnalyzer dream) {
        dream.setState(new DreamAssociation());
    }

    @Override
    public void prev(DreamAnalyzer dream) {
        dream.setState(new DreamHistory());
    }

    @Override
    public String execute(DreamAnalyzer dream, String text) {
        String response = dream.extractItems(OBJECTS_PROMPT);
        dream.setObjectList(DreamAnalyzer.extractAndSplit(response));

        return dream.getObjectList().isEmpty() ?
                ERR_MSG :
                new StringBuilder()
                        .append(DESC_MSG)
                        .append("\n\n")
                        .append(String.join("\n", dream.getObjectList()))
                        .append("\n\n")
                        .append(NEXT_MSG)
                        .toString();
    }
}