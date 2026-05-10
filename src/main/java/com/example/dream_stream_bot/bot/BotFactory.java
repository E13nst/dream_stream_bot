package com.example.dream_stream_bot.bot;

import com.example.dream_stream_bot.model.telegram.BotEntity;
import com.example.dream_stream_bot.service.telegram.MessageHandlerService;
import com.example.dream_stream_bot.service.telegram.UserStateService;
import com.example.dream_stream_bot.service.user.UserService;

public class BotFactory {
    public static AbstractTelegramBot createBot(BotEntity botEntity,
                                                MessageHandlerService messageHandlerService,
                                                UserStateService userStateService,
                                                UserService userService) {
        String type = botEntity.getType();
        if (type == null) {
            throw new IllegalArgumentException("Bot type is not specified");
        }

        // Логируем для отладки
        System.out.println("🔍 BotFactory: Создаем бота типа: '" + type + "' (username: " + botEntity.getUsername() + ")");
        
        return switch (type.toLowerCase()) {
            case "copycat", "cotycat" -> new CopyCatBot(botEntity, messageHandlerService, userService);
            case "assistant" -> new AssistantBot(botEntity, messageHandlerService, userService);
            // Добавляй новые типы ботов здесь
            default -> throw new IllegalArgumentException("Unknown bot type: " + type + ". Supported types: copycat, assistant");
        };
    }
}
