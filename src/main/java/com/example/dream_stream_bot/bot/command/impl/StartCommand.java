package com.example.dream_stream_bot.bot.command.impl;

import com.example.dream_stream_bot.bot.command.BotCommand;
import com.example.dream_stream_bot.bot.command.ChatScope;
import com.example.dream_stream_bot.bot.command.CommandContext;
import com.example.dream_stream_bot.bot.message.OutgoingMessage;
import com.example.dream_stream_bot.service.onboarding.OnboardingService;
import com.example.dream_stream_bot.service.subscription.GroupLinkWizardService;
import com.example.dream_stream_bot.service.telegram.BotNavigationService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Команда /start: в группах — deeplink для владельца; в личке — онбординг (персональный, владелец группы или участник).
 */
@Component
public class StartCommand implements BotCommand {

    private final OnboardingService onboardingService;
    private final BotNavigationService botNavigationService;
    private final GroupLinkWizardService groupLinkWizardService;

    public StartCommand(OnboardingService onboardingService,
                        BotNavigationService botNavigationService,
                        GroupLinkWizardService groupLinkWizardService) {
        this.onboardingService = onboardingService;
        this.botNavigationService = botNavigationService;
        this.groupLinkWizardService = groupLinkWizardService;
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
            String args = ctx.getArgs() == null ? "" : ctx.getArgs().trim();
            if (args.startsWith("link_group_")) {
                long telegramUserId;
                try {
                    telegramUserId = Long.parseLong(args.substring("link_group_".length()).trim());
                } catch (NumberFormatException e) {
                    return BotCommand.reply(OutgoingMessage.of(ctx.getChatId(), "Некорректная ссылка привязки группы."));
                }
                Message msg = ctx.getMessage();
                boolean samePerson = msg.getFrom() != null && msg.getFrom().getId().equals(telegramUserId);
                if (!samePerson) {
                    String hint = msg.getSenderChat() != null
                            ? "Ссылка выдана вашему личному аккаунту, а здесь сообщение не от личного профиля (часто так в обсуждении канала). Продолжите привязку из группы, где вы пишете как пользователь, или используйте другой чат для подключения."
                            : "Эта ссылка привязки выдана другому пользователю Telegram. Попросите того, кто настраивает подписку, открыть бота в личке и заново получить ссылку «Выбрать группу».";
                    return BotCommand.reply(OutgoingMessage.of(ctx.getChatId(), hint));
                }
                if (ctx.getUser() == null) {
                    return BotCommand.silent();
                }
                Long privateChatId = ctx.getUser().getTelegramId();
                List<OutgoingMessage> priv = groupLinkWizardService.handleLinkGroupStartFromGroup(
                        privateChatId, ctx.getBotEntity(), ctx.getUser(), telegramUserId, ctx.getChatId());
                Integer threadId = ctx.getMessageThreadId();
                List<OutgoingMessage> out = new ArrayList<>();
                out.add(OutgoingMessage.builder()
                        .chatId(ctx.getChatId())
                        .messageThreadId(threadId)
                        .text("Продолжите в личке с ботом — там подтверждение выбора группы.")
                        .build());
                out.addAll(priv);
                return out;
            }
            String link = "https://t.me/" + username.trim() + "?start=group_owner_" + ctx.getChatId();
            String text = """
                    Чтобы активировать групповую подписку, администратор группы должен открыть бота в личке по ссылке:
                    %s
                    
                    После активации он сможет отправить в группу приглашение со ссылкой на согласие для участников.
                    """.formatted(link);
            Integer threadId = ctx.getMessageThreadId();
            return BotCommand.reply(OutgoingMessage.builder()
                    .chatId(ctx.getChatId())
                    .messageThreadId(threadId)
                    .text(text.trim())
                    .replyMarkup(botNavigationService.groupStartKeyboard(username, ctx.getChatId()))
                    .build());
        }

        if (ctx.getUser() == null) {
            return BotCommand.silent();
        }
        return onboardingService.start(ctx.getUser(), ctx.getBotEntity(), ctx.getChatId(), ctx.getArgs());
    }
}
