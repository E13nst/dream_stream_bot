package com.example.dream_stream_bot.dream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;

class DreamPersonality implements AnalyzerState {

    private static final Logger LOGGER = LoggerFactory.getLogger(DreamPersonality.class);
    private static final String MSG_END = "Нажмите кнопку далее для продолжения";

    private String currentPerson;

    @Override
    public DreamStatus getCurrentState() {
        return DreamStatus.PERSONALITY;
    }

    @Override
    public void next(DreamAnalyzer dream) {
        dream.setState(new DreamInterpretation());
    }

    @Override
    public void prev(DreamAnalyzer dream) {
        dream.setState(new DreamActors());
    }

    @Override
    public String execute(DreamAnalyzer dream, String text) {
        if (currentPerson != null) {
            dream.getActors().put(currentPerson, text);
            LOGGER.info("PERSONS: {}", dream.getActors());
        }

        Optional<String> firstEmptyKey = dream.getActors().entrySet().stream()
                .filter(entry -> entry.getValue() == null || entry.getValue().isEmpty())
                .map(Map.Entry::getKey)
                .findFirst();

        currentPerson = firstEmptyKey.orElse(MSG_END);
        return currentPerson;
    }
}