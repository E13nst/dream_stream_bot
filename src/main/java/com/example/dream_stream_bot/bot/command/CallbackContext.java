package com.example.dream_stream_bot.bot.command;

import com.example.dream_stream_bot.model.telegram.BotEntity;
import com.example.dream_stream_bot.model.user.UserEntity;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.bots.AbsSender;

public final class CallbackContext {

    private final CallbackQuery callbackQuery;
    private final BotEntity botEntity;
    private final AbsSender sender;
    private final String botUsername;
    private final UserEntity user;
    private final String prefix;
    private final String payload;

    public CallbackContext(CallbackQuery callbackQuery, BotEntity botEntity, AbsSender sender,
                           String botUsername, UserEntity user, String prefix, String payload) {
        this.callbackQuery = callbackQuery;
        this.botEntity = botEntity;
        this.sender = sender;
        this.botUsername = botUsername;
        this.user = user;
        this.prefix = prefix;
        this.payload = payload;
    }

    public CallbackQuery getCallbackQuery() { return callbackQuery; }
    public BotEntity getBotEntity() { return botEntity; }
    public AbsSender getSender() { return sender; }
    public String getBotUsername() { return botUsername; }
    public UserEntity getUser() { return user; }
    public String getPrefix() { return prefix; }
    public String getPayload() { return payload; }

    public Long getChatId() {
        return callbackQuery != null && callbackQuery.getMessage() != null
                ? callbackQuery.getMessage().getChatId() : null;
    }

    public Long getUserTelegramId() {
        return callbackQuery != null && callbackQuery.getFrom() != null
                ? callbackQuery.getFrom().getId() : null;
    }
}
