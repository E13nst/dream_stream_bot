package com.example.dream_stream_bot.service.ai;

public interface AIService {
    String completion(String conversationId, String message, String userName, String prompt);
    String completion(String conversationId, String message, String prompt);
}
