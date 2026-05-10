package com.example.dream_stream_bot.bot;

import com.example.dream_stream_bot.model.telegram.BotEntity;
import com.example.dream_stream_bot.service.telegram.BotService;
import com.example.dream_stream_bot.service.telegram.MessageHandlerService;
import com.example.dream_stream_bot.service.telegram.UserStateService;
import com.example.dream_stream_bot.service.user.UserService;

public class BotFactory {
    public static AbstractTelegramBot createBot(BotEntity botEntity,
                                                BotService botService,
                                                MessageHandlerService messageHandlerService,
                                                UserStateService userStateService,
                                                UserService userService) {
        String type = botEntity.getType();
        if (type == null) {
            throw new IllegalArgumentException("Bot type is not specified");
        }

        Long id = botEntity.getId();
        if (id == null) {
            throw new IllegalArgumentException("Bot must be persisted (id set) before creating Telegram bot instance");
        }

        System.out.println("🔍 BotFactory: Создаем бота типа: '" + type + "' (username: " + botEntity.getUsername() + ")");

        return switch (type.toLowerCase()) {
            case "copycat", "cotycat" -> new CopyCatBot(id, botService, messageHandlerService, userService);
            case "assistant" -> new AssistantBot(id, botService, messageHandlerService, userService);
            default -> throw new IllegalArgumentException("Unknown bot type: " + type + ". Supported types: copycat, assistant");
        };
    }
}
