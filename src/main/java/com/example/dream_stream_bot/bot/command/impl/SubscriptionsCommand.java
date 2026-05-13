package com.example.dream_stream_bot.bot.command.impl;

import com.example.dream_stream_bot.bot.command.BotCommand;
import com.example.dream_stream_bot.bot.command.ChatScope;
import com.example.dream_stream_bot.bot.command.CommandContext;
import com.example.dream_stream_bot.bot.message.OutgoingMessage;
import com.example.dream_stream_bot.model.subscription.SubscriptionEntity;
import com.example.dream_stream_bot.model.subscription.SubscriptionStatus;
import com.example.dream_stream_bot.model.subscription.SubscriptionTariffEntity;
import com.example.dream_stream_bot.model.subscription.SubscriptionTariffRepository;
import com.example.dream_stream_bot.model.subscription.TariffScope;
import com.example.dream_stream_bot.model.telegram.BotEntity;
import com.example.dream_stream_bot.model.user.UserEntity;
import com.example.dream_stream_bot.service.subscription.SubscriptionCardTextBuilder;
import com.example.dream_stream_bot.service.subscription.SubscriptionService;
import com.example.dream_stream_bot.service.telegram.BotNavigationService;
import com.example.dream_stream_bot.service.telegram.TelegramGroupAdminService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Команда {@code /subscriptions} — личная и групповые подписки на текущего бота.
 */
@Component
public class SubscriptionsCommand implements BotCommand {

    private final SubscriptionService subscriptionService;
    private final SubscriptionTariffRepository subscriptionTariffRepository;
    private final SubscriptionCardTextBuilder subscriptionCardTextBuilder;
    private final BotNavigationService botNavigationService;
    private final TelegramGroupAdminService telegramGroupAdminService;

    public SubscriptionsCommand(SubscriptionService subscriptionService,
                                SubscriptionTariffRepository subscriptionTariffRepository,
                                SubscriptionCardTextBuilder subscriptionCardTextBuilder,
                                BotNavigationService botNavigationService,
                                TelegramGroupAdminService telegramGroupAdminService) {
        this.subscriptionService = subscriptionService;
        this.subscriptionTariffRepository = subscriptionTariffRepository;
        this.subscriptionCardTextBuilder = subscriptionCardTextBuilder;
        this.botNavigationService = botNavigationService;
        this.telegramGroupAdminService = telegramGroupAdminService;
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
        BotEntity bot = ctx.getBotEntity();
        if (user == null) {
            return BotCommand.reply(OutgoingMessage.builder()
                    .chatId(message.getChatId())
                    .messageThreadId(ctx.getMessageThreadId())
                    .text("Не удалось определить профиль пользователя.")
                    .build());
        }
        if (bot == null || bot.getId() == null) {
            return BotCommand.reply(OutgoingMessage.builder()
                    .chatId(message.getChatId())
                    .messageThreadId(ctx.getMessageThreadId())
                    .text("Бот не настроен. Обратитесь в поддержку.")
                    .build());
        }

        StringBuilder text = new StringBuilder();
        text.append("── Ваша подписка ──\n\n");

        Optional<SubscriptionEntity> subOpt =
                subscriptionService.findPersonal(bot.getId(), user.getId());
        if (subOpt.isEmpty()) {
            text.append(emptySubscriptionText(bot));
        } else {
            SubscriptionEntity sub = subOpt.get();
            SubscriptionTariffEntity tariff = sub.getTariffId() == null
                    ? null
                    : subscriptionTariffRepository.findById(sub.getTariffId()).orElse(null);
            text.append(subscriptionCardTextBuilder.buildPersonalCard(bot, sub, tariff));
        }

        text.append("\n\n── Групповые подписки ──\n\n");
        List<SubscriptionEntity> groupSubs = subscriptionService.findGroupByOwner(user.getId(), bot.getId());
        if (groupSubs.isEmpty()) {
            text.append("Пока нет подключённых групп. Нажмите «➕ Подключить группу» ниже.");
        } else {
            boolean first = true;
            for (SubscriptionEntity g : groupSubs) {
                if (!first) {
                    text.append("\n\n");
                }
                first = false;
                text.append(formatGroupSubscriptionLine(bot, g));
            }
        }

        return BotCommand.reply(OutgoingMessage.builder()
                .chatId(message.getChatId())
                .messageThreadId(ctx.getMessageThreadId())
                .text(text.toString().trim())
                .replyMarkup(botNavigationService.subscriptionManageInlineKeyboard())
                .build());
    }

    private String formatGroupSubscriptionLine(BotEntity bot, SubscriptionEntity g) {
        String chatTitle = g.getScopeChatId() == null
                ? "—"
                : telegramGroupAdminService.getChatTitle(bot, g.getScopeChatId())
                        .orElse("Группа #" + g.getScopeChatId());
        SubscriptionTariffEntity tariff = g.getTariffId() == null
                ? null
                : subscriptionTariffRepository.findById(g.getTariffId()).orElse(null);
        String tariffName = tariff == null ? "—" : tariff.getTitle();
        String until = g.getExpiresAt() == null ? "—" : g.getExpiresAt().toLocalDate().toString();
        String statusRu = groupStatusRu(g.getStatus());
        return "• " + chatTitle + "\n  Тариф: " + tariffName + "\n  Статус: " + statusRu + "\n  До: " + until;
    }

    private static String groupStatusRu(SubscriptionStatus st) {
        if (st == null) {
            return "—";
        }
        return switch (st) {
            case TRIAL -> "Пробный период";
            case ACTIVE -> "Активна";
            case AWAITING_ACTIVATION -> "Ожидает активации";
            default -> st.name();
        };
    }

    private String emptySubscriptionText(BotEntity bot) {
        List<SubscriptionTariffEntity> paidPersonal = subscriptionTariffRepository
                .findByBotIdAndActiveTrueAndScopeAndPriceAmountMinorIsNotNullOrderBySortOrderAscIdAsc(
                        bot.getId(), TariffScope.PERSONAL);
        StringBuilder sb = new StringBuilder();
        sb.append("Личная подписка на этого бота ещё не создана.\n\n");
        if (paidPersonal.isEmpty()) {
            sb.append("Нажмите /start, чтобы пройти онбординг.");
        } else {
            sb.append("Нажмите /start или оформите доступ кнопкой «💳 Продлить» ниже.");
        }
        if (bot.isYookassaReceiptEnabled()) {
            sb.append("\n\nДля чека по 54‑ФЗ: /billing_email ваш@example.ru");
        }
        return sb.toString().trim();
    }
}
