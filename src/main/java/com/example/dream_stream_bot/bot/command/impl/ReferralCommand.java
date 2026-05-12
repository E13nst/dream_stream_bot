package com.example.dream_stream_bot.bot.command.impl;

import com.example.dream_stream_bot.bot.command.BotCommand;
import com.example.dream_stream_bot.bot.command.ChatScope;
import com.example.dream_stream_bot.bot.command.CommandContext;
import com.example.dream_stream_bot.bot.message.OutgoingMessage;
import com.example.dream_stream_bot.model.user.UserEntity;
import com.example.dream_stream_bot.service.telegram.BotNavigationService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * {@code /referral} — персональная ссылка {@code ref_<internal_user_id>} для {@link com.example.dream_stream_bot.service.onboarding.OnboardingService}.
 */
@Component
public class ReferralCommand implements BotCommand {

    private final BotNavigationService botNavigationService;

    public ReferralCommand(BotNavigationService botNavigationService) {
        this.botNavigationService = botNavigationService;
    }

    @Override
    public String name() {
        return "referral";
    }

    @Override
    public boolean appliesIn(ChatScope scope) {
        return scope == ChatScope.PRIVATE;
    }

    @Override
    public Optional<String> menuDescription() {
        return Optional.empty();
    }

    @Override
    public Set<ChatScope> menuScopes() {
        return Set.of(ChatScope.PRIVATE);
    }

    @Override
    public List<OutgoingMessage> handle(CommandContext ctx) {
        Message message = ctx.getMessage();
        UserEntity user = ctx.getUser();
        if (user == null) {
            return BotCommand.reply(OutgoingMessage.builder()
                    .chatId(message.getChatId())
                    .messageThreadId(ctx.getMessageThreadId())
                    .text("Не удалось определить профиль пользователя.")
                    .build());
        }
        String un = ctx.getBotUsername() != null ? ctx.getBotUsername() : "";
        String link = "https://t.me/" + un + "?start=ref_" + user.getId();
        String text = """
                Ваша реферальная ссылка (приглашённый открывает бота по ней; бонус +14 дня обоим начисляется после первой реальной оплаты приглашённого, когда платежи будут включены):

                """ + link;
        return BotCommand.reply(OutgoingMessage.builder()
                .chatId(message.getChatId())
                .messageThreadId(ctx.getMessageThreadId())
                .text(text)
                .replyMarkup(botNavigationService.privateMainKeyboard())
                .build());
    }
}
