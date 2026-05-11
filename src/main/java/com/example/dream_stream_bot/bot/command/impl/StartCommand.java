package com.example.dream_stream_bot.bot.command.impl;

import com.example.dream_stream_bot.bot.command.BotCommand;
import com.example.dream_stream_bot.bot.command.ChatScope;
import com.example.dream_stream_bot.bot.command.CommandContext;
import com.example.dream_stream_bot.bot.message.OutgoingMessage;
import com.example.dream_stream_bot.service.onboarding.OnboardingService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Команда /start: в группах — deeplink для владельца; в личке — онбординг (персональный, владелец группы или участник).
 */
@Component
public class StartCommand implements BotCommand {

    private final OnboardingService onboardingService;

    public StartCommand(OnboardingService onboardingService) {
        this.onboardingService = onboardingService;
    }

    @Override
    public String name() {
        return "start";
    }

    @Override
    public boolean appliesIn(ChatScope scope) {
        return true;
    }

    @Override
    public Optional<String> menuDescription() {
        return Optional.of("Запустить бота и онбординг");
    }

    @Override
    public List<OutgoingMessage> handle(CommandContext ctx) {
        if (ctx.getBotEntity() == null || ctx.getChatId() == null) {
            return BotCommand.silent();
        }

        ChatScope scope = ctx.getChatScope();
        String username = ctx.getBotUsername();

        if (scope != ChatScope.PRIVATE) {
            if (username == null || username.isBlank()) {
                return BotCommand.reply(OutgoingMessage.of(ctx.getChatId(),
                        "Настройте username у бота, чтобы использовать групповые сценарии."));
            }
            String link = "https://t.me/" + username.trim() + "?start=group_owner_" + ctx.getChatId();
            String text = """
                    Чтобы активировать групповую подписку, администратор группы должен открыть бота в личке по ссылке:
                    %s
                    
                    После активации участники смогут принять согласие по своей персональной ссылке из приветственного сообщения владельца.
                    """.formatted(link);
            Integer threadId = ctx.getMessageThreadId();
            return BotCommand.reply(OutgoingMessage.builder()
                    .chatId(ctx.getChatId())
                    .messageThreadId(threadId)
                    .text(text.trim())
                    .build());
        }

        if (ctx.getUser() == null) {
            return BotCommand.silent();
        }
        return onboardingService.start(ctx.getUser(), ctx.getBotEntity(), ctx.getChatId(), ctx.getArgs());
    }
}
