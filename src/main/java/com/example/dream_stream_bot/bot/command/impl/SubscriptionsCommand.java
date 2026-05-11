package com.example.dream_stream_bot.bot.command.impl;

import com.example.dream_stream_bot.bot.command.BotCommand;
import com.example.dream_stream_bot.bot.command.CommandContext;
import com.example.dream_stream_bot.bot.command.ChatScope;
import com.example.dream_stream_bot.bot.message.OutgoingMessage;
import com.example.dream_stream_bot.model.subscription.SubscriptionEntity;
import com.example.dream_stream_bot.model.telegram.BotEntity;
import com.example.dream_stream_bot.model.user.UserEntity;
import com.example.dream_stream_bot.service.subscription.SubscriptionService;
import com.example.dream_stream_bot.service.telegram.BotService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Команда {@code /subscriptions} — список подписок текущего пользователя как владельца по всем ботам-помощникам.
 */
@Component
public class SubscriptionsCommand implements BotCommand {

    private final SubscriptionService subscriptionService;
    private final BotService botService;

    public SubscriptionsCommand(SubscriptionService subscriptionService, BotService botService) {
        this.subscriptionService = subscriptionService;
        this.botService = botService;
    }

    @Override
    public String name() {
        return "subscriptions";
    }

    @Override
    public boolean appliesIn(ChatScope scope) {
        return scope == ChatScope.PRIVATE;
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
        List<SubscriptionEntity> mine = subscriptionService.findOwnedBy(user.getId());
        if (mine.isEmpty()) {
            return BotCommand.reply(OutgoingMessage.builder()
                    .chatId(message.getChatId())
                    .messageThreadId(ctx.getMessageThreadId())
                    .text("У вас пока нет подписок как владельца. Откройте нужного бота и нажмите /start.")
                    .build());
        }
        Map<Long, BotEntity> botsById = new LinkedHashMap<>();
        for (BotEntity b : botService.findAll()) {
            botsById.put(b.getId(), b);
        }
        List<String> lines = new ArrayList<>();
        lines.add("Ваши подписки (как владелец):");
        for (SubscriptionEntity s : mine) {
            BotEntity b = botsById.get(s.getBotId());
            String botLabel = b != null ? "@" + b.getUsername() : "bot#" + s.getBotId();
            String scope = s.getScopeChatId() != null ? "группа " + s.getScopeChatId() : "личка";
            String exp = s.getExpiresAt() != null ? s.getExpiresAt().toLocalDate().toString() : "—";
            lines.add(String.format("• %s | %s | %s | до %s | %s",
                    botLabel, s.getPlan(), scope, exp, s.getStatus()));
        }
        lines.add("Продление — через администратора или оплату (когда будет подключена).");
        return BotCommand.reply(OutgoingMessage.builder()
                .chatId(message.getChatId())
                .messageThreadId(ctx.getMessageThreadId())
                .text(String.join("\n", lines))
                .build());
    }
}
