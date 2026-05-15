package com.example.dream_stream_bot.bot.command.impl;

import com.example.dream_stream_bot.bot.command.CallbackContext;
import com.example.dream_stream_bot.bot.command.CallbackHandler;
import com.example.dream_stream_bot.bot.message.OutgoingMessage;
import com.example.dream_stream_bot.model.subscription.SubscriptionPaymentEntity;
import com.example.dream_stream_bot.model.subscription.SubscriptionPaymentRepository;
import com.example.dream_stream_bot.model.subscription.SubscriptionPaymentStatus;
import com.example.dream_stream_bot.model.subscription.SubscriptionTariffEntity;
import com.example.dream_stream_bot.model.subscription.TariffScope;
import com.example.dream_stream_bot.model.subscription.SubscriptionTariffRepository;
import com.example.dream_stream_bot.service.payment.ReceiptEmailAwaitService;
import com.example.dream_stream_bot.service.payment.SubscriptionCheckoutService;
import com.example.dream_stream_bot.service.payment.SubscriptionPaymentCompletionService;
import com.example.dream_stream_bot.service.payment.YooKassaCheckoutOutgoingFactory;
import com.example.dream_stream_bot.service.subscription.TariffCheckoutPreviewTextBuilder;
import com.example.dream_stream_bot.service.telegram.BotNavigationService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Component
public class PaymentCallback implements CallbackHandler {

    private final SubscriptionTariffRepository tariffRepository;
    private final SubscriptionCheckoutService checkoutService;
    private final SubscriptionPaymentCompletionService completionService;
    private final SubscriptionPaymentRepository paymentRepository;
    private final BotNavigationService botNavigationService;
    private final TariffCheckoutPreviewTextBuilder tariffCheckoutPreviewTextBuilder;
    private final ReceiptEmailAwaitService receiptEmailAwaitService;
    private final YooKassaCheckoutOutgoingFactory yooKassaCheckoutOutgoingFactory;

    public PaymentCallback(SubscriptionTariffRepository tariffRepository,
                           SubscriptionCheckoutService checkoutService,
                           SubscriptionPaymentCompletionService completionService,
                           SubscriptionPaymentRepository paymentRepository,
                           BotNavigationService botNavigationService,
                           TariffCheckoutPreviewTextBuilder tariffCheckoutPreviewTextBuilder,
                           ReceiptEmailAwaitService receiptEmailAwaitService,
                           YooKassaCheckoutOutgoingFactory yooKassaCheckoutOutgoingFactory) {
        this.tariffRepository = tariffRepository;
        this.checkoutService = checkoutService;
        this.completionService = completionService;
        this.paymentRepository = paymentRepository;
        this.botNavigationService = botNavigationService;
        this.tariffCheckoutPreviewTextBuilder = tariffCheckoutPreviewTextBuilder;
        this.receiptEmailAwaitService = receiptEmailAwaitService;
        this.yooKassaCheckoutOutgoingFactory = yooKassaCheckoutOutgoingFactory;
    }

    @Override
    public String prefix() {
        return BotNavigationService.CALLBACK_PAY;
    }

    @Override
    public List<OutgoingMessage> handle(CallbackContext ctx) {
        if (ctx.getBotEntity() == null || ctx.getUser() == null) {
            return CallbackHandler.silent();
        }
        Long chatId = ctx.getChatId();
        if (chatId == null) {
            return CallbackHandler.silent();
        }
        long botId = ctx.getBotEntity().getId();
        long ownerUserId = ctx.getUser().getId();

        String payload = ctx.getPayload() == null ? "" : ctx.getPayload().trim();
        if (payload.isEmpty()) {
            return CallbackHandler.silent();
        }
        if ("list".equals(payload)) {
            return listTariffs(chatId, botId);
        }
        if ("history".equals(payload)) {
            return paymentHistory(chatId, botId, ownerUserId);
        }
        if (payload.startsWith("detail:")) {
            long tariffId = Long.parseLong(payload.substring("detail:".length()));
            return tariffDetailPreview(chatId, botId, tariffId);
        }
        if (payload.startsWith("open:")) {
            long tariffId = Long.parseLong(payload.substring("open:".length()));
            return tariffOpenCheckout(ctx, chatId, botId, ownerUserId, tariffId);
        }
        if (payload.startsWith("status:")) {
            long paymentRecordId = Long.parseLong(payload.substring("status:".length()));
            return checkStatus(chatId, paymentRecordId, ownerUserId);
        }
        return CallbackHandler.silent();
    }

    private List<OutgoingMessage> paymentHistory(Long chatId, long botId, long ownerUserId) {
        List<SubscriptionPaymentEntity> rows =
                paymentRepository.findTop15ByBotIdAndOwnerUserIdOrderByCreatedAtDesc(botId, ownerUserId);
        if (rows.isEmpty()) {
            return List.of(OutgoingMessage.builder()
                    .chatId(chatId)
                    .text("Пока нет сохранённых платежей по этому боту.")
                    .replyMarkup(botNavigationService.subscriptionHistoryBackKeyboard())
                    .build());
        }
        StringBuilder sb = new StringBuilder("История платежей (последние записи):\n\n");
        for (SubscriptionPaymentEntity p : rows) {
            String date = p.getCreatedAt() != null
                    ? p.getCreatedAt().toLocalDate().toString()
                    : "—";
            sb.append("• ").append(date)
                    .append(" · ").append(formatRub(p.getAmountMinor())).append(" ₽")
                    .append(" · ").append(statusRu(p.getStatus()))
                    .append("\n");
        }
        return List.of(OutgoingMessage.builder()
                .chatId(chatId)
                .text(sb.toString().trim())
                .replyMarkup(botNavigationService.subscriptionHistoryBackKeyboard())
                .build());
    }

    private static String statusRu(SubscriptionPaymentStatus s) {
        if (s == null) {
            return "—";
        }
        return switch (s) {
            case PENDING -> "в обработке";
            case SUCCEEDED -> "успешно";
            case FAILED -> "ошибка";
            case CANCELLED -> "отменён";
        };
    }

    private List<OutgoingMessage> listTariffs(Long chatId, long botId) {
        List<SubscriptionTariffEntity> tariffs = tariffRepository
                .findByBotIdAndActiveTrueAndScopeAndPriceAmountMinorIsNotNullOrderBySortOrderAscIdAsc(
                        botId, TariffScope.PERSONAL);
        if (tariffs.isEmpty()) {
            return List.of(OutgoingMessage.builder()
                    .chatId(chatId)
                    .text("Нет тарифов с ценой для оплаты в этом боте. Обратитесь к поддержке.")
                    .replyMarkup(botNavigationService.privateMainKeyboard())
                    .build());
        }
        InlineKeyboardMarkup.InlineKeyboardMarkupBuilder kb = InlineKeyboardMarkup.builder();
        for (SubscriptionTariffEntity t : tariffs) {
            String rub = formatRub(t.getPriceAmountMinor());
            kb.keyboardRow(List.of(InlineKeyboardButton.builder()
                    .text(t.getTitle() + " — " + rub + " ₽")
                    .callbackData(BotNavigationService.CALLBACK_PAY + ":detail:" + t.getId())
                    .build()));
        }
        return List.of(OutgoingMessage.builder()
                .chatId(chatId)
                .text("Выберите тариф для оплаты:")
                .replyMarkup(kb.build())
                .build());
    }

    /** Условия тарифа без вызова ЮKassa; оплата — отдельным нажатием {@code pay:open:…}. */
    private List<OutgoingMessage> tariffDetailPreview(Long chatId, long botId, long tariffId) {
        SubscriptionTariffEntity tariff = loadPayablePersonalTariff(botId, tariffId);
        if (tariff == null) {
            return List.of(OutgoingMessage.builder()
                    .chatId(chatId)
                    .text("Этот тариф недоступен. Выберите другой вариант.")
                    .replyMarkup(botNavigationService.privateMainKeyboard())
                    .build());
        }
        InlineKeyboardMarkup kb = InlineKeyboardMarkup.builder()
                .keyboardRow(List.of(InlineKeyboardButton.builder()
                        .text("💳 Оплатить через ЮKassa")
                        .callbackData(BotNavigationService.CALLBACK_PAY + ":open:" + tariffId)
                        .build()))
                .keyboardRow(List.of(InlineKeyboardButton.builder()
                        .text("⬅ К тарифам")
                        .callbackData(BotNavigationService.CALLBACK_PAY + ":list")
                        .build()))
                .build();
        return List.of(OutgoingMessage.builder()
                .chatId(chatId)
                .text(tariffCheckoutPreviewTextBuilder.buildPersonalCheckoutPreviewText(tariff))
                .replyMarkup(kb)
                .build());
    }

    /** Создание платежа в ЮKassa; ответ — новым сообщением (ссылка, «Я оплатил», назад к списку). */
    private List<OutgoingMessage> tariffOpenCheckout(CallbackContext ctx, Long chatId, long botId, long ownerUserId, long tariffId) {
        if (loadPayablePersonalTariff(botId, tariffId) == null) {
            return List.of(OutgoingMessage.builder()
                    .chatId(chatId)
                    .text("Этот тариф недоступен. Выберите другой вариант.")
                    .replyMarkup(botNavigationService.privateMainKeyboard())
                    .build());
        }
        try {
            var botEntity = ctx.getBotEntity();
            var userEntity = ctx.getUser();
            if (Boolean.TRUE.equals(botEntity.isYookassaReceiptEnabled())) {
                String billing = userEntity.getBillingEmail();
                if (billing == null || billing.isBlank()) {
                    receiptEmailAwaitService.startAwaitingPersonal(botId, ownerUserId, tariffId);
                    return receiptEmailAwaitService.promptMessages(chatId, callbackMessageThreadId(ctx), false);
                }
            }
            SubscriptionCheckoutService.CheckoutResult result =
                    checkoutService.createCheckout(botId, ownerUserId, tariffId);
            return yooKassaCheckoutOutgoingFactory.personalTariffPaymentReady(
                    chatId, callbackMessageThreadId(ctx), result);
        } catch (RuntimeException e) {
            return List.of(OutgoingMessage.builder()
                    .chatId(chatId)
                    .text("Не удалось создать платёж: " + e.getMessage())
                    .replyMarkup(InlineKeyboardMarkup.builder()
                            .keyboardRow(List.of(InlineKeyboardButton.builder()
                                    .text("⬅ К тарифам")
                                    .callbackData(BotNavigationService.CALLBACK_PAY + ":list")
                                    .build()))
                            .build())
                    .build());
        }
    }

    /** {@code null}, если тариф не из этого бота, неактивен, не персональный или без цены. */
    private SubscriptionTariffEntity loadPayablePersonalTariff(long botId, long tariffId) {
        SubscriptionTariffEntity tariff = tariffRepository.findById(tariffId).orElse(null);
        if (tariff == null || !tariff.getBotId().equals(botId) || Boolean.FALSE.equals(tariff.isActive())
                || tariff.getPriceAmountMinor() == null || tariff.getScope() != TariffScope.PERSONAL) {
            return null;
        }
        return tariff;
    }

    private List<OutgoingMessage> checkStatus(Long chatId, long paymentRecordId, long ownerUserId) {
        boolean completed = completionService.completeByLocalPaymentId(paymentRecordId, ownerUserId);
        if (completed) {
            return List.of(OutgoingMessage.builder()
                    .chatId(chatId)
                    .text("Оплата подтверждена. Доступ по подписке активен.")
                    .replyMarkup(botNavigationService.privateMainKeyboard())
                    .build());
        }
        return List.of(OutgoingMessage.builder()
                .chatId(chatId)
                .text("Платёж ещё не в статусе «успешно» или данные обрабатываются. Подождите минуту и попробуйте снова.")
                .replyMarkup(botNavigationService.privateMainKeyboard())
                .build());
    }

    private static Integer callbackMessageThreadId(CallbackContext ctx) {
        if (ctx.getCallbackQuery() == null || !(ctx.getCallbackQuery().getMessage() instanceof Message msg)) {
            return null;
        }
        return Boolean.TRUE.equals(msg.getIsTopicMessage()) ? msg.getMessageThreadId() : null;
    }

    private static String formatRub(Long minor) {
        if (minor == null) {
            return "?";
        }
        return BigDecimal.valueOf(minor).divide(BigDecimal.valueOf(100), 2, RoundingMode.UNNECESSARY).toPlainString();
    }
}
