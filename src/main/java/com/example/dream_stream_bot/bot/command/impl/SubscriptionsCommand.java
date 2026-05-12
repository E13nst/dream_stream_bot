package com.example.dream_stream_bot.bot.command.impl;

import com.example.dream_stream_bot.bot.command.BotCommand;
import com.example.dream_stream_bot.bot.command.CommandContext;
import com.example.dream_stream_bot.bot.command.ChatScope;
import com.example.dream_stream_bot.bot.message.OutgoingMessage;
import com.example.dream_stream_bot.model.subscription.SubscriptionEntity;
import com.example.dream_stream_bot.model.subscription.SubscriptionTariffEntity;
import com.example.dream_stream_bot.model.subscription.SubscriptionTariffRepository;
import com.example.dream_stream_bot.model.telegram.BotEntity;
import com.example.dream_stream_bot.model.user.UserEntity;
import com.example.dream_stream_bot.service.subscription.SubscriptionService;
import com.example.dream_stream_bot.service.telegram.BotNavigationService;
import com.example.dream_stream_bot.service.telegram.BotService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Команда {@code /subscriptions} — список подписок текущего пользователя как владельца по всем ботам-помощникам.
 */
@Component
public class SubscriptionsCommand implements BotCommand {

    private final SubscriptionService subscriptionService;
    private final BotService botService;
    private final SubscriptionTariffRepository subscriptionTariffRepository;
    private final BotNavigationService botNavigationService;

    public SubscriptionsCommand(SubscriptionService subscriptionService,
                                BotService botService,
                                SubscriptionTariffRepository subscriptionTariffRepository,
                                BotNavigationService botNavigationService) {
        this.subscriptionService = subscriptionService;
        this.botService = botService;
        this.subscriptionTariffRepository = subscriptionTariffRepository;
        this.botNavigationService = botNavigationService;
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
    public Optional<String> menuDescription() {
        return Optional.of("Мои подписки");
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
        Set<Long> tariffIds = new HashSet<>();
        for (SubscriptionEntity subscription : mine) {
            if (subscription.getTariffId() != null) {
                tariffIds.add(subscription.getTariffId());
            }
        }
        Map<Long, SubscriptionTariffEntity> tariffById = subscriptionTariffRepository
                .findAllById(tariffIds)
                .stream()
                .collect(Collectors.toMap(SubscriptionTariffEntity::getId, t -> t));
        for (SubscriptionEntity s : mine) {
            BotEntity b = botsById.get(s.getBotId());
            String botLabel = b != null ? "@" + b.getUsername() : "bot#" + s.getBotId();
            SubscriptionTariffEntity t = tariffById.get(s.getTariffId());
            String tariffLabel = t != null ? t.getCode() + ": " + t.getTitle() : "тариф#" + s.getTariffId();
            String scope = s.getScopeChatId() != null ? "группа " + s.getScopeChatId() : "личка";
            String exp = s.getExpiresAt() != null ? s.getExpiresAt().toLocalDate().toString() : "—";
            lines.add(String.format("• %s | %s | %s | до %s | %s",
                    botLabel, tariffLabel, scope, exp, s.getStatus()));
        }
        lines.add("Продление: после принятия документов используйте «💳 Оплатить подписку» в ⚙ Настройки или кнопку с экрана онбординга. Команда /billing_email — email для чека (если включено у бота).");
        return BotCommand.reply(OutgoingMessage.builder()
                .chatId(message.getChatId())
                .messageThreadId(ctx.getMessageThreadId())
                .text(String.join("\n", lines))
                .replyMarkup(botNavigationService.privateMainKeyboard())
                .build());
    }
}
