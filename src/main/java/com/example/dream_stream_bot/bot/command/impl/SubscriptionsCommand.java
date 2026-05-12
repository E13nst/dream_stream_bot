package com.example.dream_stream_bot.bot.command.impl;

import com.example.dream_stream_bot.bot.command.BotCommand;
import com.example.dream_stream_bot.bot.command.ChatScope;
import com.example.dream_stream_bot.bot.command.CommandContext;
import com.example.dream_stream_bot.bot.message.OutgoingMessage;
import com.example.dream_stream_bot.model.subscription.SubscriptionEntity;
import com.example.dream_stream_bot.model.subscription.SubscriptionTariffEntity;
import com.example.dream_stream_bot.model.subscription.SubscriptionTariffRepository;
import com.example.dream_stream_bot.model.subscription.TariffScope;
import com.example.dream_stream_bot.model.telegram.BotEntity;
import com.example.dream_stream_bot.model.user.UserEntity;
import com.example.dream_stream_bot.service.subscription.SubscriptionCardTextBuilder;
import com.example.dream_stream_bot.service.subscription.SubscriptionService;
import com.example.dream_stream_bot.service.telegram.BotNavigationService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Команда {@code /subscriptions} — карточка личной подписки на <em>текущего</em> бота.
 */
@Component
public class SubscriptionsCommand implements BotCommand {

    private final SubscriptionService subscriptionService;
    private final SubscriptionTariffRepository subscriptionTariffRepository;
    private final SubscriptionCardTextBuilder subscriptionCardTextBuilder;
    private final BotNavigationService botNavigationService;

    public SubscriptionsCommand(SubscriptionService subscriptionService,
                                SubscriptionTariffRepository subscriptionTariffRepository,
                                SubscriptionCardTextBuilder subscriptionCardTextBuilder,
                                BotNavigationService botNavigationService) {
        this.subscriptionService = subscriptionService;
        this.subscriptionTariffRepository = subscriptionTariffRepository;
        this.subscriptionCardTextBuilder = subscriptionCardTextBuilder;
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

        Optional<SubscriptionEntity> subOpt =
                subscriptionService.findPersonal(bot.getId(), user.getId());
        if (subOpt.isEmpty()) {
            String text = emptySubscriptionText(bot);
            return BotCommand.reply(OutgoingMessage.builder()
                    .chatId(message.getChatId())
                    .messageThreadId(ctx.getMessageThreadId())
                    .text(text)
                    .replyMarkup(botNavigationService.subscriptionManageInlineKeyboard())
                    .build());
        }

        SubscriptionEntity sub = subOpt.get();
        SubscriptionTariffEntity tariff = sub.getTariffId() == null
                ? null
                : subscriptionTariffRepository.findById(sub.getTariffId()).orElse(null);
        String text = subscriptionCardTextBuilder.buildPersonalCard(bot, sub, tariff);

        return BotCommand.reply(OutgoingMessage.builder()
                .chatId(message.getChatId())
                .messageThreadId(ctx.getMessageThreadId())
                .text(text)
                .replyMarkup(botNavigationService.subscriptionManageInlineKeyboard())
                .build());
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
            sb.append("Нажмите /start или оформите доступ кнопкой «Продлить или оплатить» ниже.");
        }
        if (bot.isYookassaReceiptEnabled()) {
            sb.append("\n\nДля чека по 54‑ФЗ: /billing_email ваш@example.ru");
        }
        return sb.toString().trim();
    }
}
