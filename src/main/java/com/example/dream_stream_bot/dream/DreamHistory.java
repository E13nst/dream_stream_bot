package com.example.dream_stream_bot.dream;

import com.example.dream_stream_bot.model.InlineButtons;
import com.example.dream_stream_bot.model.InlineCommandKeyboard;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

import java.util.ArrayList;
import java.util.List;

class DreamHistory implements AnalyzerState {

    private static final String HISTORY_DESCRIPTION = "На первом шаге я помогу вам записать ваш сон в виде истории. " +
            "Постарайтесь описать его как можно подробнее, включая все детали, которые помните. Не беспокойтесь, " +
            "если сначала ваш рассказ будет состоять из отдельных фрагментов или элементов. На данном этапе важно " +
            "собрать всю информацию, даже если она " +
            "кажется разрозненной. Вы можете отправлять описание сна в нескольких сообщениях. После того как вы закончите, " +
            "нажмите кнопку **Продолжить**, чтобы перейти к следующему этапу анализа.";

    @Override
    public DreamStatus getState() {
        return DreamStatus.HISTORY;
    }

    @Override
    public void next(DreamAnalyzer analyzer) {

        var dream = analyzer.getDream();

        dream.addAllElements(AiTextProcessor.extractElements(
                analyzer.getOpenaiChat(),
                analyzer.getUserName(),
                analyzer.getDream().getHistoryStr()));

        dream.addAllActors(AiTextProcessor.extractActors(
                analyzer.getOpenaiChat(),
                analyzer.getUserName(),
                analyzer.getDream().getHistoryStr()));

        analyzer.setState(new DreamAssociation());
    }

    @Override
    public void prev(DreamAnalyzer analyzer) {
        analyzer.setState(new DreamNew());
    }

    @Override
    public List<SendMessage> run(DreamAnalyzer analyzer, String text) {

        List<SendMessage> messages = new ArrayList<>();

        if (analyzer.getDream().getHistoryStr().isEmpty()) {
            SendMessage message = analyzer.newTelegramMessage(HISTORY_DESCRIPTION);
            messages.add(message);
        }

        analyzer.getDream().addHistory(text);
        return messages;
    }
}