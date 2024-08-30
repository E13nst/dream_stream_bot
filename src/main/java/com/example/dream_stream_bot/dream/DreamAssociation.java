package com.example.dream_stream_bot.dream;

import com.example.dream_stream_bot.model.InlineButtons;
import com.example.dream_stream_bot.model.InlineCommandKeyboard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

class DreamAssociation implements AnalyzerState {

    private static final Logger LOGGER = LoggerFactory.getLogger(DreamAssociation.class);

    private static final String MSG_DESC_1 = "Я выберу из твоего рассказа образы и предметы для подбора ассоциации.";
    private static final String MSG_DESC_2 = "Напиши, что каждый конкретный образ значит для тебя в контексте сна. " +
            "Если каждый образ вызывает несколько ассоциаций или воспоминаний — например, конкретного человека, " +
            "слова, фразы или ситуации — запиши все эти мысли.";
    private static final String MSG_DESC_3 = "Не переживай о правильности ассоциаций на этом этапе. Важно собрать разные варианты, " +
            "даже если они кажутся несвязанными. Наша цель — найти прямые ассоциации, которые возникают в связи с каждым образом.";
    private static final String MSG_DESC_4 = "Подбери ассоциации к образу: ";
    private static final String MSG_END = "У нас получились такие ассоциации:";

    private final Iterator<DreamElement> iterator;
    private DreamElement currentElement;

    private final InlineKeyboardMarkup keyboardMarkup = new InlineCommandKeyboard()
            .addKey("Продолжить \u2705", InlineButtons.NEXT.toString())
            .addKey("Отмена \u274C", InlineButtons.CANCEL.toString())
            .build();

    DreamAssociation(DreamAnalyzer analyzer) {
        this.iterator = analyzer.getDream().getAssociations().iterator();
    }

    @Override
    public DreamStatus getState() {
        return DreamStatus.ASSOCIATION;
    }

    @Override
    public List<SendMessage> next(DreamAnalyzer analyzer) {
        analyzer.setState(new DreamPersonality(analyzer));
        return null;
    }

    @Override
    public void prev(DreamAnalyzer analyzer) {
        analyzer.setState(new DreamHistory());
    }

    @Override
    public List<SendMessage> run(DreamAnalyzer analyzer, String msg) {

        List<SendMessage> messages = new ArrayList<>();

        if (currentElement == null) {
            messages.add(analyzer.newTelegramMessage(MSG_DESC_1));
            messages.add(analyzer.newTelegramMessage(MSG_DESC_2));
            messages.add(analyzer.newTelegramMessage(MSG_DESC_3));
            messages.add(analyzer.newTelegramMessage(MSG_DESC_4));
        } else if (msg != null && !msg.isBlank()) {
            currentElement.setAssociation(msg);
            LOGGER.info("Association set for element {}", currentElement);
        }

        if (iterator.hasNext()) {
            currentElement = iterator.next();
            messages.add(analyzer.newTelegramMessage(currentElement.getName()));
        }
        else {
            messages.add(analyzer.newTelegramMessage(MSG_END));
            messages.add(analyzer.newTelegramMessage(analyzer.getDream().associationsCollectToString(), keyboardMarkup));
        }
        return messages;
    }

}