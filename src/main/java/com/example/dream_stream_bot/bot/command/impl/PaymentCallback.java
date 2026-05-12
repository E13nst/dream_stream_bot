package com.example.dream_stream_bot.bot.command.impl;

import com.example.dream_stream_bot.bot.command.CallbackContext;
import com.example.dream_stream_bot.bot.command.CallbackHandler;
import com.example.dream_stream_bot.bot.message.OutgoingMessage;
import com.example.dream_stream_bot.model.subscription.SubscriptionTariffEntity;
import com.example.dream_stream_bot.model.subscription.TariffScope;
import com.example.dream_stream_bot.model.subscription.SubscriptionTariffRepository;
import com.example.dream_stream_bot.service.payment.SubscriptionCheckoutService;
import com.example.dream_stream_bot.service.payment.SubscriptionPaymentCompletionService;
import com.example.dream_stream_bot.service.telegram.BotNavigationService;
import org.springframework.stereotype.Component;
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
    private final BotNavigationService botNavigationService;

    public PaymentCallback(SubscriptionTariffRepository tariffRepository,
                           SubscriptionCheckoutService checkoutService,
                           SubscriptionPaymentCompletionService completionService,
                           BotNavigationService botNavigationService) {
        this.tariffRepository = tariffRepository;
        this.checkoutService = checkoutService;
        this.completionService = completionService;
        this.botNavigationService = botNavigationService;
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
        if (payload.startsWith("open:")) {
            long tariffId = Long.parseLong(payload.substring("open:".length()));
            return openCheckout(chatId, botId, ownerUserId, tariffId);
        }
        if (payload.startsWith("status:")) {
            long paymentRecordId = Long.parseLong(payload.substring("status:".length()));
            return checkStatus(chatId, paymentRecordId, ownerUserId);
        }
        return CallbackHandler.silent();
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
                    .callbackData(BotNavigationService.CALLBACK_PAY + ":open:" + t.getId())
                    .build()));
        }
        return List.of(OutgoingMessage.builder()
                .chatId(chatId)
                .text("Выберите тариф для оплаты:")
                .replyMarkup(kb.build())
                .build());
    }

    private List<OutgoingMessage> openCheckout(Long chatId, long botId, long ownerUserId, long tariffId) {
        try {
            SubscriptionCheckoutService.CheckoutResult result =
                    checkoutService.createCheckout(botId, ownerUserId, tariffId);
            InlineKeyboardMarkup kb = InlineKeyboardMarkup.builder()
                    .keyboardRow(List.of(InlineKeyboardButton.builder()
                            .text("💳 Перейти к оплате")
                            .url(result.confirmationUrl())
                            .build()))
                    .keyboardRow(List.of(InlineKeyboardButton.builder()
                            .text("✅ Проверить оплату")
                            .callbackData(BotNavigationService.CALLBACK_PAY + ":status:" + result.localPaymentId())
                            .build()))
                    .build();
            return List.of(OutgoingMessage.builder()
                    .chatId(chatId)
                    .text("""
                            Откройте оплату кнопкой ниже. После успешной оплаты доступ активируется автоматически в течение короткого времени.

                            Если статус не обновился — нажмите «Проверить оплату».""")
                    .replyMarkup(kb)
                    .build());
        } catch (RuntimeException e) {
            return List.of(OutgoingMessage.builder()
                    .chatId(chatId)
                    .text("Не удалось создать платёж: " + e.getMessage())
                    .replyMarkup(botNavigationService.privateMainKeyboard())
                    .build());
        }
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

    private static String formatRub(Long minor) {
        if (minor == null) {
            return "?";
        }
        return BigDecimal.valueOf(minor).divide(BigDecimal.valueOf(100), 2, RoundingMode.UNNECESSARY).toPlainString();
    }
}
