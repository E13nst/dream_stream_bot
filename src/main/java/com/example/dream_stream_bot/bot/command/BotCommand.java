package com.example.dream_stream_bot.bot.command;

import com.example.dream_stream_bot.bot.message.OutgoingMessage;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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

    /**
     * Описание команды для меню Telegram (выпадающего по «/»).
     * Пустой {@link Optional} означает, что команда не должна публиковаться в меню
     * (например, callback-обработчики или служебные).
     *
     * Telegram требует 1..256 символов; короткие, аккуратные формулировки.
     */
    default Optional<String> menuDescription() {
        return Optional.empty();
    }

    /**
     * Области чатов, в которых команда отображается в меню Telegram.
     * По умолчанию — везде, где она применима ({@link #appliesIn(ChatScope)}).
     *
     * Меняется отдельно от {@link #appliesIn(ChatScope)}, чтобы можно было
     * скрыть пункт из меню группового чата, оставив выполнение по явному вводу.
     */
    default Set<ChatScope> menuScopes() {
        return Set.of(ChatScope.PRIVATE, ChatScope.GROUP, ChatScope.SUPERGROUP);
    }

    /** Удобный helper для пустого ответа. */
    static List<OutgoingMessage> silent() {
        return Collections.emptyList();
    }

    /** Удобный helper для одного ответа. */
    static List<OutgoingMessage> reply(OutgoingMessage message) {
        return List.of(message);
    }
}
