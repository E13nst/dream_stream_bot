package com.example.dream_stream_bot.service.payment;

import com.example.dream_stream_bot.bot.message.OutgoingMessage;
import com.example.dream_stream_bot.service.telegram.BotNavigationService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.List;

/**
 * Единообразные ответы после успешного создания платежа ЮKassa (ссылка, «Я оплатил», назад).
 */
@Component
public class YooKassaCheckoutOutgoingFactory {

    private static final String PAYMENT_READY_BODY = """
            Ссылка на оплату готова ниже.

            После успешной оплаты доступ включается автоматически. Если этого не случилось (например, при локальном запуске без веб-хука) — нажмите «Я оплатил»."""
            .strip();

    private final BotNavigationService botNavigationService;

    public YooKassaCheckoutOutgoingFactory(BotNavigationService botNavigationService) {
        this.botNavigationService = botNavigationService;
    }

    public List<OutgoingMessage> personalTariffPaymentReady(long chatId, Integer messageThreadId,
                                                            SubscriptionCheckoutService.CheckoutResult result) {
        InlineKeyboardMarkup kb = InlineKeyboardMarkup.builder()
                .keyboardRow(List.of(InlineKeyboardButton.builder()
                        .text("💳 Оплатить через ЮKassa")
                        .url(result.confirmationUrl())
                        .build()))
                .keyboardRow(List.of(InlineKeyboardButton.builder()
                        .text("✅ Я оплатил")
                        .callbackData(BotNavigationService.CALLBACK_PAY + ":status:" + result.localPaymentId())
                        .build()))
                .keyboardRow(List.of(InlineKeyboardButton.builder()
                        .text("⬅ К тарифам")
                        .callbackData(BotNavigationService.CALLBACK_PAY + ":list")
                        .build()))
                .build();
        return List.of(OutgoingMessage.builder()
                .chatId(chatId)
                .messageThreadId(messageThreadId)
                .text(PAYMENT_READY_BODY)
                .replyMarkup(kb)
                .build());
    }

    public List<OutgoingMessage> groupSubscriptionPaymentReady(long chatId, Integer messageThreadId,
                                                               SubscriptionCheckoutService.CheckoutResult result) {
        InlineKeyboardMarkup kb = InlineKeyboardMarkup.builder()
                .keyboardRow(List.of(InlineKeyboardButton.builder()
                        .text("💳 Оплатить через ЮKassa")
                        .url(result.confirmationUrl())
                        .build()))
                .keyboardRow(List.of(InlineKeyboardButton.builder()
                        .text("✅ Я оплатил")
                        .callbackData(BotNavigationService.CALLBACK_PAY + ":status:" + result.localPaymentId())
                        .build()))
                .keyboardRow(List.of(InlineKeyboardButton.builder()
                        .text("⬅ К подписке")
                        .callbackData(botNavigationService.navPayload("subscriptions"))
                        .build()))
                .build();
        return List.of(OutgoingMessage.builder()
                .chatId(chatId)
                .messageThreadId(messageThreadId)
                .text(PAYMENT_READY_BODY)
                .replyMarkup(kb)
                .build());
    }
}
