package com.example.dream_stream_bot.bot.command;

import com.example.dream_stream_bot.model.telegram.BotEntity;
import com.example.dream_stream_bot.model.user.UserEntity;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.bots.AbsSender;

/**
 * Контекст вызова команды.
 * Содержит распарсенные удобные ссылки на {@link Update}, чат, пользователя
 * и сам бот-экземпляр (как {@link AbsSender}) для прямой отправки в случае
 * нестандартных flows.
 */
public final class CommandContext {

    private final Update update;
    private final Message message;
    private final BotEntity botEntity;
    private final AbsSender sender;
    private final String botUsername;
    private final UserEntity user;
    private final String command;
    private final String args;
    private final ChatScope chatScope;

    public CommandContext(Update update,
                          Message message,
                          BotEntity botEntity,
                          AbsSender sender,
                          String botUsername,
                          UserEntity user,
                          String command,
                          String args,
                          ChatScope chatScope) {
        this.update = update;
        this.message = message;
        this.botEntity = botEntity;
        this.sender = sender;
        this.botUsername = botUsername;
        this.user = user;
        this.command = command;
        this.args = args;
        this.chatScope = chatScope;
    }

    public Update getUpdate() { return update; }
    public Message getMessage() { return message; }
    public BotEntity getBotEntity() { return botEntity; }
    public AbsSender getSender() { return sender; }
    public String getBotUsername() { return botUsername; }
    public UserEntity getUser() { return user; }
    public String getCommand() { return command; }
    public String getArgs() { return args; }
    public ChatScope getChatScope() { return chatScope; }

    public Long getChatId() {
        return message != null ? message.getChatId() : null;
    }

    public Integer getMessageThreadId() {
        if (message == null) {
            return null;
        }
        return Boolean.TRUE.equals(message.getIsTopicMessage()) ? message.getMessageThreadId() : null;
    }

    public Integer getMessageId() {
        return message != null ? message.getMessageId() : null;
    }
}
