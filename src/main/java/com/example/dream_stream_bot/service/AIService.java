package com.example.dream_stream_bot.service;

import com.example.dream_stream_bot.model.Dream;

import java.util.List;

public interface AIService {

    String completion(long chatId, String message);

    String completion(long chatId, String message, String userName);

    String findElements(long chatId, String text);

    String findActors(long chatId, String text);

    List<String> extractAndSplitItems(long chatId, String text, String userName, String prompt);

    List<String> extractElements(long chatId, String text, String userName);

    List<String> extractActors(long chatId, String text, String userName);

    String interpretDream(long chatId, String userName, Dream dream);

    String interpretDream(long chatId, Dream dream);

}
