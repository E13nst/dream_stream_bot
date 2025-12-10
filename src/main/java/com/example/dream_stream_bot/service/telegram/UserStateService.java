package com.example.dream_stream_bot.service.telegram;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UserStateService {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(UserStateService.class);
    
    private final Map<Long, UserState> userStates = new ConcurrentHashMap<>();
    
    public enum UserState {
    }
    
    public UserState getUserState(Long userId) {
        return userStates.getOrDefault(userId, null);
    }
    
    public void setUserState(Long userId, UserState state) {
        LOGGER.info("🔧 Устанавливаем состояние для пользователя {}: {}", userId, state);
        userStates.put(userId, state);
    }
    
    public void clearUserState(Long userId) {
        LOGGER.info("🧹 Очищаем состояние пользователя: {}", userId);
        userStates.remove(userId);
    }
} 