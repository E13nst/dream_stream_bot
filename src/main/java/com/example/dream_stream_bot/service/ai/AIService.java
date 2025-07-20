package com.example.dream_stream_bot.service.ai;

public interface AIService {

    String completion(long chatId, String message);

    String completion(long chatId, String message, String userName);

}
