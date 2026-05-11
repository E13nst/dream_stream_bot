package com.example.dream_stream_bot.bot;

import com.example.dream_stream_bot.bot.command.CallbackDispatcher;
import com.example.dream_stream_bot.bot.command.CommandDispatcher;
import com.example.dream_stream_bot.bot.error.BotUpdateErrorHandler;
import com.example.dream_stream_bot.bot.message.MessageSender;
import com.example.dream_stream_bot.model.telegram.BotEntity;
import com.example.dream_stream_bot.service.access.AccessGate;
import com.example.dream_stream_bot.service.access.GatingDedup;
import com.example.dream_stream_bot.service.telegram.BotService;
import com.example.dream_stream_bot.service.telegram.MessageHandlerService;
import com.example.dream_stream_bot.service.telegram.UserStateService;
import com.example.dream_stream_bot.service.user.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BotFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(BotFactory.class);

    private BotFactory() {
    }

    public static AbstractTelegramBot createBot(BotEntity botEntity,
                                                BotService botService,
                                                MessageHandlerService messageHandlerService,
                                                UserStateService userStateService,
                                                UserService userService,
                                                MessageSender messageSender,
                                                CommandDispatcher commandDispatcher,
                                                CallbackDispatcher callbackDispatcher,
                                                BotUpdateErrorHandler errorHandler,
                                                EditedMessageHandler editedMessageHandler,
                                                AccessGate accessGate,
                                                GatingDedup gatingDedup) {
        String type = botEntity.getType();
        if (type == null) {
            throw new IllegalArgumentException("Bot type is not specified");
        }

        Long id = botEntity.getId();
        if (id == null) {
            throw new IllegalArgumentException("Bot must be persisted (id set) before creating Telegram bot instance");
        }

        LOGGER.info("🔍 BotFactory: creating bot type='{}', username='{}'", type, botEntity.getUsername());

        return switch (type.toLowerCase()) {
            case "copycat", "cotycat" -> new CopyCatBot(id, botService, messageHandlerService, userService,
                    messageSender, commandDispatcher, callbackDispatcher, errorHandler, editedMessageHandler);
            case "assistant" -> new AssistantBot(id, botService, messageHandlerService, userService,
                    messageSender, commandDispatcher, callbackDispatcher, errorHandler, editedMessageHandler,
                    accessGate, gatingDedup);
            default -> throw new IllegalArgumentException("Unknown bot type: " + type + ". Supported types: copycat, assistant");
        };
    }
}
