package com.example.dream_stream_bot.dream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.util.*;

class DreamPersonality implements AnalyzerState {

    private static final Logger LOGGER = LoggerFactory.getLogger(DreamPersonality.class);

    private static final String MSG_DESC_1 = "Я выделил из твоей истории таких персонажей:";
    private static final String MSG_DESC_2 = "Какая черта твоей личности ассоциируется с этим персонажем? " +
            "Где в реальной жизни она проявляется? Что эта черта значит для тебя?";
    private static final String MSG_DESC_3 = "Подберите ассоциации для слова:";

    private static final String MSG_FAIL = "Я не смог выделить из твоей истории персонажей.";
    private static final String MSG_END = "Нажмите кнопку далее для продолжения";

    private final Deque<String> persons = new ArrayDeque<>();
    private String currentPerson;

    @Override
    public DreamStatus getState() {
        return DreamStatus.PERSONALITY;
    }

    @Override
    public void next(DreamAnalyzer analyzer) {
        analyzer.setState(new DreamInterpretation());
    }

    @Override
    public void prev(DreamAnalyzer analyzer) {
        analyzer.setState(new DreamAssociation());
    }

    @Override
    public List<SendMessage> init(DreamAnalyzer analyzer) {

        List<SendMessage> messages = new ArrayList<>();

        persons.addAll(AiTextProcessor.extractActors(
                analyzer.getOpenaiChat(),
                analyzer.getUserName(),
                analyzer.getDream().getHistoryStr()));

        if (persons.isEmpty()) {
            LOGGER.warn("No elements found in dream analysis");
            messages.add(analyzer.newTelegramMessage(MSG_FAIL));
        } else {
            messages.add(analyzer.newTelegramMessage(MSG_DESC_1));
            messages.add(analyzer.newTelegramMessage(getMsgPersons()));
            messages.add(analyzer.newTelegramMessage(MSG_DESC_2));
            messages.add(analyzer.newTelegramMessage(MSG_DESC_3));
        }
        return messages;
    }

    @Override
    public List<SendMessage> execute(DreamAnalyzer analyzer, String text) {

        List<SendMessage> messages = new ArrayList<>();

        if (text != null && !text.isBlank()) {
            analyzer.getDream().putAssociation(currentPerson, text);
            LOGGER.info("Association set for element {}: {}", currentPerson, text);
        } else {
            LOGGER.warn("Received blank message, skipping association");
        }

        currentPerson = persons.pollFirst();
        SendMessage sendMessage = analyzer.newTelegramMessage(Objects.requireNonNullElse(currentPerson, MSG_END));
        messages.add(sendMessage);
        return messages;
    }

    private String getMsgPersons() {
        return String.join("\n", persons);
    }
}