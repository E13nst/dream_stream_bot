package com.example.dream_stream_bot.service;

import com.example.dream_stream_bot.config.DreamStateConfig;
import com.example.dream_stream_bot.model.DreamActor;
import com.example.dream_stream_bot.model.Dream;
import com.example.dream_stream_bot.model.DreamState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DreamServiceImpl implements DreamService {

    private final DreamStateConfig dreamStateConfig;

    @Autowired
    AIService aiService;

    @Autowired
    public DreamServiceImpl(DreamStateConfig dreamStateConfig) {
        this.dreamStateConfig = dreamStateConfig;
    }

    private final Map<Long, Dream> userDreams = new HashMap<>();

    @Override
    public String getDreamText(long userId) {
        return userDreams.getOrDefault(userId, new Dream()).getHistory();
    }

    @Override
    public void addDreamText(long userId, String dreamText) {
        Dream dream = userDreams.computeIfAbsent(userId, k -> new Dream());
        dream.addToHistory(dreamText);
    }

    @Override
    public String getFirstUnassociatedDreamElement(long userId) {
        return userDreams.computeIfAbsent(userId, k -> new Dream()).getFirstUnassociatedDreamElement();
    }

    @Override
    public void addDreamActor(long userId, DreamActor actor) {
        Dream dream = userDreams.computeIfAbsent(userId, k -> new Dream());
        dream.addActor(actor);
    }

    @Override
    public void setCurrentDreamAssociation(long userId, String association) {
        Dream dream = userDreams.computeIfAbsent(userId, k -> new Dream());
        dream.setCurrentAssociation(association);
    }

    @Override
    public Dream getUserDream(long userId) {
        return userDreams.getOrDefault(userId, null);
    }

    @Override
    public Dream removeUserDream(long userId) {
        return userDreams.remove(userId);
    }

    @Override
    public void changeDreamState(long userId, DreamState newState) {
        Dream dream = userDreams.getOrDefault(userId, new Dream());
        dream.changeState(newState);
    }

    @Override
    public String interpretUserDream(long userId) {
        Dream dream = userDreams.get(userId);

        if (dream != null) {
            return aiService.interpretDream(userId, dream);
        }
        return null;
    }

    @Override
    public String interpretUserDream(long userId, String userName) {
        Dream dream = userDreams.get(userId);

        if (dream != null) {
            String result = aiService.interpretDream(userId, userName, dream);
            dream.changeState(DreamState.COMPLETE);
            return result;
        }
        return null;
    }

    @Override
    public String getDreamCurrentStateDescription(long userId) {
        Dream dream = userDreams.getOrDefault(userId, null);
        return dream != null ? dreamStateConfig.getDescription(dream.getCurrentState().name()) : null;
    }

    @Override
    public String getDreamStateDescription(DreamState dreamState) {
        return dreamStateConfig.getDescription(dreamState.name());
    }

    @Override
    public DreamState getNextDreamState(DreamState currentDreamState) {
        DreamState[] states = DreamState.values();
        int currentIndex = currentDreamState.ordinal();
        if (currentIndex < states.length)
            currentIndex++;
        return states[currentIndex];
    }

    @Override
    public String create(Long userId) {
        userDreams.put(userId, new Dream());
        return getDreamStateDescription(DreamState.HISTORY);
    }

    @Override
    public String findDreamElements(Long userId) {

        Dream dream = userDreams.get(userId);

        String rawText = aiService.findElements(userId, dream.getHistory());
        List<String> elements = AIServiceImpl.splitItems(rawText);

        if (elements.isEmpty()) {
            return rawText;
        }

        elements.forEach(dream::addElement);
        dream.changeState(DreamState.ASSOCIATION);

        String description = getDreamCurrentStateDescription(userId);
        String element = dream.getFirstUnassociatedDreamElement();
        return String.format("%s\n\n- *%s*:", description, element);
    }

    @Override
    public String findDreamActors(Long userId) {

        Dream dream = userDreams.get(userId);

        String rawText = aiService.findActors(userId, dream.getHistory());
        List<String> elements = AIServiceImpl.splitItems(rawText);

        if (elements.isEmpty()) { // TODO добавить выброс exception
            return rawText;
        }

        elements.forEach(e -> dream.addActor(new DreamActor(e)));
        dream.changeState(DreamState.PERSONALITY);

        return String.format("%s\n- *%s*:",
                getDreamCurrentStateDescription(userId),
                dream.getNextActor().getPerson());
    }

    @Override
    public String stepDescription(Long userId) {

        StringBuilder descBuilder = new StringBuilder();

        if (getDreamCurrentStateDescription(userId) != null) {
            descBuilder.append(getDreamCurrentStateDescription(userId));
        }

        Dream dream = userDreams.getOrDefault(userId, null);

        if (dream != null) {

            String firstElement = switch (dream.getCurrentState()) {
                case ASSOCIATION -> dream.getFirstUnassociatedDreamElement();
                case PERSONALITY -> dream.getNextActor().getPerson();
                case CONTEXT, SENSE -> dream.getNextActor().getCharacteristic();
                default -> null;
            };

            if (firstElement != null) {
                descBuilder.append(String.format("\n- *%s*:", firstElement));
            }
        }

        return descBuilder.toString();
    }

}

