package com.example.dream_stream_bot.service;

import com.example.dream_stream_bot.config.DreamStateConfig;
import com.example.dream_stream_bot.model.DreamActor;
import com.example.dream_stream_bot.model.Dream;
import com.example.dream_stream_bot.model.DreamState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DreamServiceImpl implements DreamService {

    private static final Logger logger = LoggerFactory.getLogger(DreamServiceImpl.class);

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
        String text = userDreams.getOrDefault(userId, new Dream()).getHistory();
        logger.debug("📖 Retrieved dream text | User: {} | Length: {} chars", userId, text.length());
        return text;
    }

    @Override
    public void addDreamText(long userId, String dreamText) {
        Dream dream = userDreams.computeIfAbsent(userId, k -> {
            logger.info("🌙 Created new dream session | User: {}", userId);
            return new Dream();
        });
        dream.addToHistory(dreamText);
        logger.info("📝 Added dream text | User: {} | Length: {} chars", userId, dreamText.length());
    }

    @Override
    public String getFirstUnassociatedDreamElement(long userId) {
        String element = userDreams.computeIfAbsent(userId, k -> new Dream()).getFirstUnassociatedDreamElement();
        logger.debug("🔍 Retrieved unassociated element | User: {} | Element: '{}'", userId, element);
        return element;
    }

    @Override
    public void addDreamActor(long userId, DreamActor actor) {
        Dream dream = userDreams.computeIfAbsent(userId, k -> new Dream());
        dream.addActor(actor);
        logger.info("👤 Added dream actor | User: {} | Actor: '{}'", userId, actor.getPerson());
    }

    @Override
    public void setCurrentDreamAssociation(long userId, String association) {
        Dream dream = userDreams.computeIfAbsent(userId, k -> new Dream());
        dream.setCurrentAssociation(association);
        logger.info("💭 Set dream association | User: {} | Association: '{}'", userId, truncateText(association, 50));
    }

    @Override
    public Dream getUserDream(long userId) {
        Dream dream = userDreams.getOrDefault(userId, null);
        logger.debug("📋 Retrieved user dream | User: {} | Exists: {}", userId, dream != null);
        return dream;
    }

    @Override
    public Dream removeUserDream(long userId) {
        Dream dream = userDreams.remove(userId);
        logger.info("🗑️ Removed user dream | User: {} | Existed: {}", userId, dream != null);
        return dream;
    }

    @Override
    public void changeDreamState(long userId, DreamState newState) {
        Dream dream = userDreams.getOrDefault(userId, new Dream());
        DreamState oldState = dream.getCurrentState();
        dream.changeState(newState);
        logger.info("🔄 Changed dream state | User: {} | {} → {}", userId, oldState, newState);
    }

    @Override
    public String interpretUserDream(long userId) {
        Dream dream = userDreams.get(userId);

        if (dream != null) {
            logger.info("🔮 Starting dream interpretation | User: {} | State: {}", userId, dream.getCurrentState());
            return aiService.interpretDream(userId, dream);
        } else {
            logger.warn("⚠️ No dream found for interpretation | User: {}", userId);
            return null;
        }
    }

    @Override
    public String interpretUserDream(long userId, String userName) {
        Dream dream = userDreams.get(userId);

        if (dream != null) {
            logger.info("🔮 Starting dream interpretation | User: {} ({}) | State: {}", userId, userName, dream.getCurrentState());
            String result = aiService.interpretDream(userId, userName, dream);
            dream.changeState(DreamState.COMPLETE);
            logger.info("✅ Dream interpretation completed | User: {} ({}) | Result length: {} chars", userId, userName, result.length());
            return result;
        } else {
            logger.warn("⚠️ No dream found for interpretation | User: {} ({})", userId, userName);
            return null;
        }
    }

    @Override
    public String getDreamCurrentStateDescription(long userId) {
        Dream dream = userDreams.getOrDefault(userId, null);
        String description = dream != null ? dreamStateConfig.getDescription(dream.getCurrentState().name()) : null;
        logger.debug("📄 Retrieved state description | User: {} | State: {} | Description: '{}'", 
            userId, dream != null ? dream.getCurrentState() : "null", truncateText(description, 50));
        return description;
    }

    @Override
    public String getDreamStateDescription(DreamState dreamState) {
        String description = dreamStateConfig.getDescription(dreamState.name());
        logger.debug("📄 Retrieved state description | State: {} | Description: '{}'", 
            dreamState, truncateText(description, 50));
        return description;
    }

    @Override
    public DreamState getNextDreamState(DreamState currentDreamState) {
        DreamState[] states = DreamState.values();
        int currentIndex = currentDreamState.ordinal();
        if (currentIndex < states.length)
            currentIndex++;
        DreamState nextState = states[currentIndex];
        logger.debug("➡️ Next dream state | Current: {} | Next: {}", currentDreamState, nextState);
        return nextState;
    }

    @Override
    public String create(Long userId) {
        userDreams.put(userId, new Dream());
        String description = getDreamStateDescription(DreamState.HISTORY);
        logger.info("🌙 Created new dream session | User: {} | Initial state: {}", userId, DreamState.HISTORY);
        return description;
    }

    @Override
    public String findDreamElements(Long userId) {
        logger.info("🔍 Finding dream elements | User: {}", userId);

        Dream dream = userDreams.get(userId);

        String rawText = aiService.findElements(userId, dream.getHistory());
        List<String> elements = AIServiceImpl.splitItems(rawText);

        if (elements.isEmpty()) {
            logger.warn("⚠️ No elements found in dream | User: {} | Raw response: '{}'", userId, truncateText(rawText, 100));
            return rawText;
        }

        elements.forEach(dream::addElement);
        dream.changeState(DreamState.ASSOCIATION);

        String description = getDreamCurrentStateDescription(userId);
        String element = dream.getFirstUnassociatedDreamElement();
        
        logger.info("✅ Found {} dream elements | User: {} | Next element: '{}'", elements.size(), userId, element);
        
        return String.format("%s\n\n- *%s*:", description, element);
    }

    @Override
    public String findDreamActors(Long userId) {
        logger.info("👥 Finding dream actors | User: {}", userId);

        Dream dream = userDreams.get(userId);

        String rawText = aiService.findActors(userId, dream.getHistory());
        List<String> elements = AIServiceImpl.splitItems(rawText);

        if (elements.isEmpty()) { // TODO добавить выброс exception
            logger.warn("⚠️ No actors found in dream | User: {} | Raw response: '{}'", userId, truncateText(rawText, 100));
            return rawText;
        }

        elements.forEach(e -> dream.addActor(new DreamActor(e)));
        dream.changeState(DreamState.PERSONALITY);

        logger.info("✅ Found {} dream actors | User: {} | Next actor: '{}'", elements.size(), userId, dream.getNextActor().getPerson());

        return String.format("%s\n- *%s*:",
                getDreamCurrentStateDescription(userId),
                dream.getNextActor().getPerson());
    }

    @Override
    public String stepDescription(Long userId) {
        logger.debug("📋 Getting step description | User: {}", userId);

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

        String description = descBuilder.toString();
        logger.debug("📋 Step description | User: {} | State: {} | Description: '{}'", 
            userId, dream != null ? dream.getCurrentState() : "null", truncateText(description, 100));
        
        return description;
    }

    private String truncateText(String text, int maxLength) {
        if (text == null) return "null";
        return text.length() <= maxLength ? text : text.substring(0, maxLength) + "...";
    }
}

