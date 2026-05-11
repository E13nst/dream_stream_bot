package com.example.dream_stream_bot.bot.command;

import com.example.dream_stream_bot.bot.message.OutgoingMessage;

import java.util.Collections;
import java.util.List;

/**
 * Команда бота. Реализация — Spring {@code @Component},
 * {@link CommandDispatcher} собирает их автоматически по {@link #name()}.
 *
 * Возвращаемый список — это очередь исходящих сообщений; пустой список означает,
 * что команда обработана молчанием.
 */
public interface BotCommand {

    /**
     * Имя команды без слэша и без суффикса {@code @bot_username}.
     * Например, для {@code /start@MyBot args} имя — {@code "start"}.
     */
    String name();

    /**
     * Можно ли вызывать команду в данном типе чата.
     * По умолчанию — везде.
     */
    default boolean appliesIn(ChatScope scope) {
        return true;
    }

    List<OutgoingMessage> handle(CommandContext ctx);

    /** Удобный helper для пустого ответа. */
    static List<OutgoingMessage> silent() {
        return Collections.emptyList();
    }

    /** Удобный helper для одного ответа. */
    static List<OutgoingMessage> reply(OutgoingMessage message) {
        return List.of(message);
    }
}
