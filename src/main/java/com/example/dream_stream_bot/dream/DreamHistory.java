package com.example.dream_stream_bot.dream;

import com.example.dream_stream_bot.model.InlineButtons;
import com.example.dream_stream_bot.model.InlineCommandKeyboard;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

class DreamHistory implements AnalyzerState {

    private static final String MSG_DESC_1 = "На первом шаге я помогу записать твой сон в виде истории.";
    private static final String MSG_DESC_2 = "Постарайся описать его как можно подробнее, включая все детали, которые помнишь. " +
            "Не беспокойся, если сначала рассказ будет состоять из отдельных фрагментов. На данном этапе важно собрать " +
            "всю информацию, даже если она кажется разрозненной.";
    private static final String MSG_DESC_3 = "Можешь отправлять описание сна в нескольких сообщениях. Когда будешь готов, " +
            "нажми кнопку **Продолжить**, чтобы перейти к следующему этапу анализа.";
    private static final String MSG_NEXT = "|\u2705| Для перехода к следующему шагу нажми \"Продолжить\"";

    @Override
    public DreamStatus getState() {
        return DreamStatus.HISTORY;
    }

    @Override
    public List<SendMessage> next(DreamAnalyzer analyzer) {

        List<SendMessage> sendMessages = new ArrayList<>();
        Dream dream = analyzer.getDream();

        if (dream.getHistoryStr().isEmpty())
            return sendMessages;

        String rawText = AiTextProcessor.findElements(
                analyzer.getOpenaiChat(),
                analyzer.getUserName(),
                analyzer.getDream().getHistoryStr());

        var elements = AiTextProcessor.splitItems(rawText);

        if (elements.isEmpty()) {
            dream.cleanHistory();
            sendMessages.add(analyzer.newTelegramMessage(rawText));
            return sendMessages;
        }

        dream.addAllElements(elements);

        var actorsList = AiTextProcessor.extractActors(
                analyzer.getOpenaiChat(),
                analyzer.getUserName(),
                analyzer.getDream().getHistoryStr());

        dream.addAllActors(actorsList.stream().map(DreamActor::new).collect(Collectors.toList()));

        if (!dream.getElements().isEmpty()) {
            analyzer.setState(new DreamAssociation());
        }

        return sendMessages;
    }

    @Override
    public void prev(DreamAnalyzer analyzer) {
        analyzer.setState(new DreamNew());
    }

    @Override
    public List<SendMessage> run(DreamAnalyzer analyzer, String text) {

        List<SendMessage> messages = new ArrayList<>();

        if (analyzer.getDream().getHistoryStr().isEmpty()) {
            messages.add(analyzer.newTelegramMessage(MSG_DESC_1));
            messages.add(analyzer.newTelegramMessage(MSG_DESC_2));
            messages.add(analyzer.newTelegramMessage(MSG_DESC_3));
        } else {
            var keyboard = new InlineCommandKeyboard()
                    .addKey("Продолжить \u2705", InlineButtons.NEXT.toString())
                    .addKey("Отмена \u274C", InlineButtons.CANCEL.toString())
                    .build();
            SendMessage message = analyzer.newTelegramMessage(MSG_NEXT, keyboard);
            messages.add(message);
        }

        analyzer.getDream().addHistory(text);
        return messages;
    }
}