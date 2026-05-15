package com.example.dream_stream_bot.service.payment;

import com.example.dream_stream_bot.bot.message.OutgoingMessage;
import com.example.dream_stream_bot.model.telegram.BotEntity;
import com.example.dream_stream_bot.model.user.UserEntity;
import com.example.dream_stream_bot.service.telegram.BotNavigationService;
import com.example.dream_stream_bot.service.user.UserService;
import com.example.dream_stream_bot.util.EmailPatterns;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.List;
import java.util.Optional;

/**
 * Ожидание email в личке после нажатия «Оплатить», если у бота включён чек и в профиле нет {@code billing_email}.
 */
@Service
public class ReceiptEmailAwaitService {

    public static final String PROMPT_TEXT = """
            Для чека по 54‑ФЗ отправьте следующим сообщением только адрес email (одна строка, например name@example.ru).

            Либо заранее сохраните его командой: /billing_email name@example.ru — затем снова нажмите «Оплатить»."""
            .strip();

    private final ReceiptEmailAwaitHolder holder;
    private final SubscriptionCheckoutService checkoutService;
    private final UserService userService;
    private final YooKassaCheckoutOutgoingFactory checkoutOutgoingFactory;
    private final BotNavigationService botNavigationService;

    public ReceiptEmailAwaitService(ReceiptEmailAwaitHolder holder,
                                    SubscriptionCheckoutService checkoutService,
                                    UserService userService,
                                    YooKassaCheckoutOutgoingFactory checkoutOutgoingFactory,
                                    BotNavigationService botNavigationService) {
        this.holder = holder;
        this.checkoutService = checkoutService;
        this.userService = userService;
        this.checkoutOutgoingFactory = checkoutOutgoingFactory;
        this.botNavigationService = botNavigationService;
    }

    public void startAwaitingPersonal(long botId, long appUserId, long tariffId) {
        holder.startAwaitingPersonal(botId, appUserId, tariffId);
    }

    public void startAwaitingGroup(long botId, long appUserId, long subscriptionId) {
        holder.startAwaitingGroup(botId, appUserId, subscriptionId);
    }

    public void clearAwaiting(long botId, long appUserId) {
        holder.clear(botId, appUserId);
    }

    public List<OutgoingMessage> promptMessages(long chatId, Integer messageThreadId, boolean groupContext) {
        return List.of(OutgoingMessage.builder()
                .chatId(chatId)
                .messageThreadId(messageThreadId)
                .text(PROMPT_TEXT)
                .replyMarkup(awaitBackKeyboard(groupContext))
                .build());
    }

    private InlineKeyboardMarkup awaitBackKeyboard(boolean groupContext) {
        return groupContext
                ? InlineKeyboardMarkup.builder()
                .keyboardRow(List.of(InlineKeyboardButton.builder()
                        .text("⬅ К подписке")
                        .callbackData(botNavigationService.navPayload("subscriptions"))
                        .build()))
                .build()
                : InlineKeyboardMarkup.builder()
                .keyboardRow(List.of(InlineKeyboardButton.builder()
                        .text("⬅ К тарифам")
                        .callbackData(BotNavigationService.CALLBACK_PAY + ":list")
                        .build()))
                .build();
    }

    /**
     * @return non-empty если активна сессия ожидания email (включая истёкшую или невалидный ввод — чтобы не уходить в LLM)
     */
    public Optional<List<OutgoingMessage>> tryHandlePlainText(Long chatId,
                                                             Integer messageThreadId,
                                                             BotEntity bot,
                                                             UserEntity user,
                                                             String text) {
        if (user == null || bot == null || text == null) {
            return Optional.empty();
        }
        Optional<ReceiptEmailAwaitHolder.Session> sessionOpt = holder.get(bot.getId(), user.getId());
        if (sessionOpt.isEmpty()) {
            return Optional.empty();
        }
        ReceiptEmailAwaitHolder.Session session = sessionOpt.get();
        if (holder.isExpired(session)) {
            holder.clear(bot.getId(), user.getId());
            return Optional.of(List.of(OutgoingMessage.builder()
                    .chatId(chatId)
                    .messageThreadId(messageThreadId)
                    .text("Время ожидания email истекло. Нажмите «Оплатить» ещё раз.")
                    .replyMarkup(botNavigationService.privateMainKeyboard())
                    .build()));
        }
        String trimmed = text.trim();
        if (!EmailPatterns.isValidBillingEmail(trimmed)) {
            boolean group = session.getKind() == ReceiptEmailAwaitHolder.Kind.GROUP_SUBSCRIPTION;
            return Optional.of(List.of(OutgoingMessage.builder()
                    .chatId(chatId)
                    .messageThreadId(messageThreadId)
                    .text("Нужен корректный email одной строкой (как name@example.ru), без пояснений в том же сообщении.\n\n"
                            + PROMPT_TEXT)
                    .replyMarkup(awaitBackKeyboard(group))
                    .build()));
        }
        try {
            SubscriptionCheckoutService.CheckoutResult result = switch (session.getKind()) {
                case PERSONAL_TARIFF -> checkoutService.createCheckout(
                        bot.getId(), user.getId(), session.getPersonalTariffId(), trimmed);
                case GROUP_SUBSCRIPTION -> checkoutService.createCheckoutForGroupSubscription(
                        bot.getId(), user.getId(), session.getGroupSubscriptionId(), trimmed);
            };
            userService.updateBillingEmail(user.getId(), trimmed);
            holder.clear(bot.getId(), user.getId());
            return Optional.of(switch (session.getKind()) {
                case PERSONAL_TARIFF -> checkoutOutgoingFactory.personalTariffPaymentReady(
                        chatId, messageThreadId, result);
                case GROUP_SUBSCRIPTION -> checkoutOutgoingFactory.groupSubscriptionPaymentReady(
                        chatId, messageThreadId, result);
            });
        } catch (RuntimeException e) {
            return Optional.of(List.of(OutgoingMessage.builder()
                    .chatId(chatId)
                    .messageThreadId(messageThreadId)
                    .text("Не удалось создать платёж: " + e.getMessage())
                    .replyMarkup(botNavigationService.privateMainKeyboard())
                    .build()));
        }
    }
}
