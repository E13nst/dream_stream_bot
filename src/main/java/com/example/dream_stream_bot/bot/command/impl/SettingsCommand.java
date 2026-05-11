package com.example.dream_stream_bot.bot.command.impl;

import com.example.dream_stream_bot.bot.command.BotCommand;
import com.example.dream_stream_bot.bot.command.ChatScope;
import com.example.dream_stream_bot.bot.command.CommandContext;
import com.example.dream_stream_bot.bot.message.OutgoingMessage;
import com.example.dream_stream_bot.service.telegram.BotNavigationService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
public class SettingsCommand implements BotCommand {

    private final BotNavigationService botNavigationService;

    public SettingsCommand(BotNavigationService botNavigationService) {
        this.botNavigationService = botNavigationService;
    }

    @Override
    public String name() {
        return "settings";
    }

    @Override
    public boolean appliesIn(ChatScope scope) {
        return scope == ChatScope.PRIVATE;
    }

    @Override
    public Optional<String> menuDescription() {
        return Optional.of("Настройки и профиль");
    }

    @Override
    public Set<ChatScope> menuScopes() {
        return Set.of(ChatScope.PRIVATE);
    }

    @Override
    public List<OutgoingMessage> handle(CommandContext ctx) {
        return BotCommand.reply(OutgoingMessage.builder()
                .chatId(ctx.getMessage().getChatId())
                .messageThreadId(ctx.getMessageThreadId())
                .text("""
                        ⚙️ Настройки

                        Здесь личные действия: подписка, реферальная ссылка и управление данными.
                        """.trim())
                .replyMarkup(botNavigationService.privateSettingsInlineKeyboard())
                .build());
    }
}
