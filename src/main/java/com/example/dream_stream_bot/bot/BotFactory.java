package com.example.dream_stream_bot.bot;

import com.example.dream_stream_bot.model.telegram.BotEntity;
import com.example.dream_stream_bot.service.telegram.MessageHandlerService;

public class BotFactory {
    public static AbstractTelegramBot createBot(BotEntity botEntity, MessageHandlerService messageHandlerService) {
        String type = botEntity.getType();
        if (type == null) {
            throw new IllegalArgumentException("Bot type is not specified");
        }
        return switch (type.toLowerCase()) {
            case "cotycat" -> new CopyCatBot(botEntity, messageHandlerService);
            case "assistant" -> new AssistantBot(botEntity, messageHandlerService);
            // Добавляй новые типы ботов здесь
            default -> throw new IllegalArgumentException("Unknown bot type: " + type);
        };
    }
} 