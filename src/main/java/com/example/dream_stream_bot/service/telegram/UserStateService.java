package com.example.dream_stream_bot.service.telegram;

import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UserStateService {
    
    private final Map<Long, UserState> userStates = new ConcurrentHashMap<>();
    private final Map<Long, StickerPackData> userData = new ConcurrentHashMap<>();
    
    public enum UserState {
        WAITING_FOR_PACK_TITLE,
        WAITING_FOR_PACK_NAME
    }
    
    public static class StickerPackData {
        private String title;
        private String name;
        
        public StickerPackData() {}
        
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }
    
    public void setUserState(Long userId, UserState state) {
        if (state == null) {
            userStates.remove(userId);
            userData.remove(userId);
        } else {
            userStates.put(userId, state);
        }
    }
    
    public UserState getUserState(Long userId) {
        return userStates.get(userId);
    }
    
    public void setStickerPackData(Long userId, StickerPackData data) {
        userData.put(userId, data);
    }
    
    public StickerPackData getStickerPackData(Long userId) {
        return userData.get(userId);
    }
    
    public void clearUserState(Long userId) {
        userStates.remove(userId);
        userData.remove(userId);
    }
} 