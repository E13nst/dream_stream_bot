package com.example.dream_stream_bot.bot.command;

/**
 * Контекст чата, в котором обрабатывается команда.
 * Используется командой для самоописания применимости через {@link BotCommand#appliesIn(ChatScope)}.
 */
public enum ChatScope {
    PRIVATE,
    GROUP,
    SUPERGROUP,
    CHANNEL,
    UNKNOWN;

    public boolean isGroupLike() {
        return this == GROUP || this == SUPERGROUP;
    }

    public static ChatScope fromMessageType(boolean isPrivate, boolean isGroup, boolean isSuperGroup, boolean isChannel) {
        if (isPrivate) {
            return PRIVATE;
        }
        if (isSuperGroup) {
            return SUPERGROUP;
        }
        if (isGroup) {
            return GROUP;
        }
        if (isChannel) {
            return CHANNEL;
        }
        return UNKNOWN;
    }
}
