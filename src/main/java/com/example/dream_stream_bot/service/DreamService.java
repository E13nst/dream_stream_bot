package com.example.dream_stream_bot.service;

import com.example.dream_stream_bot.model.DreamActor;
import com.example.dream_stream_bot.model.Dream;
import com.example.dream_stream_bot.model.DreamState;

public interface DreamService {
    void addDreamText(long userId, String dreamText);

    String getDreamText(long userId);

    String getFirstUnassociatedDreamElement(long userId);

    void addDreamActor(long userId, DreamActor actor);

    void setCurrentDreamAssociation(long userId, String association);

    Dream getUserDream(long userId);

    Dream removeUserDream(long userId);

    void changeDreamState(long userId, DreamState state);

    String interpretUserDream(long userId);

    String interpretUserDream(long userId, String userName);

    String getDreamCurrentStateDescription(long userId);

    String getDreamStateDescription(DreamState currentState);

    DreamState getNextDreamState(DreamState currentDreamState);

    String create(Long userId);

    String findDreamElements(Long userId);

    String findDreamActors(Long userId);

    String stepDescription(Long userId);


}