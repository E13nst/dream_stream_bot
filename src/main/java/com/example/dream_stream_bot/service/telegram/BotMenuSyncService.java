package com.example.dream_stream_bot.service.telegram;

import com.example.dream_stream_bot.bot.command.BotCommand;
import com.example.dream_stream_bot.bot.command.ChatScope;
import com.example.dream_stream_bot.model.telegram.BotEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Синхронизация набора команд бота с меню Telegram через {@code setMyCommands}.
 *
 * Публикует две области:
 * <ul>
 *     <li>{@code all_private_chats} — команды, у которых {@link BotCommand#menuScopes()} содержит {@link ChatScope#PRIVATE}.</li>
 *     <li>{@code all_group_chats} — команды, у которых {@link BotCommand#menuScopes()} содержит {@link ChatScope#GROUP} или {@link ChatScope#SUPERGROUP}.</li>
 * </ul>
 *
 * В обе области попадают только команды с непустым {@link BotCommand#menuDescription()}.
 * Идемпотентно: вызовы можно делать на каждом старте без побочных эффектов.
 */
@Service
public class BotMenuSyncService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BotMenuSyncService.class);

    private static final Map<String, Object> SCOPE_PRIVATE = Map.of("type", "all_private_chats");
    private static final Map<String, Object> SCOPE_GROUP = Map.of("type", "all_group_chats");

    private final List<BotCommand> commands;
    private final TelegramBotApiService telegramBotApiService;

    public BotMenuSyncService(List<BotCommand> commands,
                              TelegramBotApiService telegramBotApiService) {
        this.commands = commands;
        this.telegramBotApiService = telegramBotApiService;
    }

    /**
     * Синхронизирует меню команд для одного бота (private + group scopes).
     * Ошибка вызова Telegram API логируется, но не пробрасывается, чтобы
     * не блокировать запуск приложения.
     */
    public void syncFor(BotEntity bot) {
        if (bot == null) {
            return;
        }
        List<TelegramBotApiService.MenuCommand> privateMenu = menuFor(Set.of(ChatScope.PRIVATE));
        List<TelegramBotApiService.MenuCommand> groupMenu = menuFor(Set.of(ChatScope.GROUP, ChatScope.SUPERGROUP));

        LOGGER.info("📋 Sync Telegram menu for bot '{}': private={} group={}",
                bot.getUsername(),
                privateMenu.stream().map(TelegramBotApiService.MenuCommand::command).toList(),
                groupMenu.stream().map(TelegramBotApiService.MenuCommand::command).toList());

        telegramBotApiService.setMyCommands(bot, privateMenu, SCOPE_PRIVATE, null);
        telegramBotApiService.setMyCommands(bot, groupMenu, SCOPE_GROUP, null);
    }

    private List<TelegramBotApiService.MenuCommand> menuFor(Set<ChatScope> targetScopes) {
        return commands.stream()
                .filter(c -> c.menuDescription().isPresent())
                .filter(c -> !java.util.Collections.disjoint(c.menuScopes(), targetScopes))
                .sorted(Comparator.comparing(BotCommand::name))
                .map(c -> new TelegramBotApiService.MenuCommand(c.name(), c.menuDescription().orElseThrow()))
                .toList();
    }
}
