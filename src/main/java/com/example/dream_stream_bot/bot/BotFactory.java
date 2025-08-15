package com.example.dream_stream_bot.bot;

import com.example.dream_stream_bot.model.telegram.BotEntity;
import com.example.dream_stream_bot.service.telegram.MessageHandlerService;
import com.example.dream_stream_bot.service.telegram.UserStateService;
import com.example.dream_stream_bot.service.telegram.StickerPackService;
import com.example.dream_stream_bot.service.telegram.StickerService;


public class BotFactory {
    public static AbstractTelegramBot createBot(BotEntity botEntity, MessageHandlerService messageHandlerService, 
                                              UserStateService userStateService, StickerPackService stickerPackService,
                                              StickerService stickerService) {
        String type = botEntity.getType();
        if (type == null) {
            throw new IllegalArgumentException("Bot type is not specified");
        }
        
        // Логируем для отладки
        System.out.println("🔍 BotFactory: Создаем бота типа: '" + type + "' (username: " + botEntity.getUsername() + ")");
        
        return switch (type.toLowerCase()) {
            case "cotycat" -> new CopyCatBot(botEntity, messageHandlerService);
            case "assistant" -> new AssistantBot(botEntity, messageHandlerService);
            case "sticker" -> new StickerBot(botEntity, messageHandlerService, userStateService, stickerPackService, stickerService);
            // Добавляй новые типы ботов здесь
            default -> {
                // Fallback: если тип не распознан, но username содержит "sticker", создаем StickerBot
                if (botEntity.getUsername() != null && botEntity.getUsername().toLowerCase().contains("sticker")) {
                    System.out.println("🔄 Fallback: Создаем StickerBot для username '" + botEntity.getUsername() + "' с типом '" + type + "'");
                    yield new StickerBot(botEntity, messageHandlerService, userStateService, stickerPackService, stickerService);
                }
                throw new IllegalArgumentException("Unknown bot type: " + type + ". Supported types: cotycat, assistant, sticker");
            }
        };
    }
} 