package com.example.dream_stream_bot.service.telegram;

import com.example.dream_stream_bot.model.keyboard.ReplyCommandKeyboard;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.List;

@Service
public class BotNavigationService {

    public static final String BTN_START = "▶ Начать";
    public static final String BTN_SETTINGS = "⚙\uFE0F Настройки";
    public static final String BTN_SUBSCRIPTION = "\uD83D\uDC8E Подписка";

    public static final String CALLBACK_NAV = "nav";

    /**
     * Префикс callback для оплаты ЮKassa:
     * {@code pay:list}, {@code pay:detail:tariffId} (только экран условий), {@code pay:open:tariffId} (создание платежа),
     * {@code pay:status:paymentId}.
     */
    public static final String CALLBACK_PAY = "pay";

    /** Мастер привязки группы: {@code grp:begin}, {@code grp:pick:tariffId}, … */
    public static final String CALLBACK_GRP = "grp";

    public ReplyKeyboard privateMainKeyboard() {
        return new ReplyCommandKeyboard()
                .addKey(BTN_SETTINGS)
                .addKey(BTN_SUBSCRIPTION)
                .build();
    }

    public InlineKeyboardMarkup privateSettingsInlineKeyboard() {
        return InlineKeyboardMarkup.builder()
                .keyboardRow(List.of(button("💎 Управление подписками", navPayload("subscriptions"))))
                .keyboardRow(List.of(button("\uD83D\uDD17 Пригласить друга", navPayload("referral"))))
                .keyboardRow(List.of(button("⬅ В главное меню", navPayload("main"))))
                .build();
    }

    public InlineKeyboardMarkup groupStartKeyboard(String botUsername, Long groupChatId) {
        String openPrivate = "https://t.me/" + botUsername.trim() + "?start=group_owner_" + groupChatId;
        InlineKeyboardButton open = InlineKeyboardButton.builder()
                .text("▶ Открыть бота в личке")
                .url(openPrivate)
                .build();
        return InlineKeyboardMarkup.builder()
                .keyboardRow(List.of(open))
                .build();
    }

    public String navPayload(String action) {
        return CALLBACK_NAV + ":" + action;
    }

    /** Кнопки под экраном подписок: оплата личной, история, группа, настройки. */
    public InlineKeyboardMarkup subscriptionManageInlineKeyboard() {
        return InlineKeyboardMarkup.builder()
                .keyboardRow(List.of(button("💳 Продлить", CALLBACK_PAY + ":list")))
                .keyboardRow(List.of(button("📊 История", CALLBACK_PAY + ":history")))
                .keyboardRow(List.of(button("➕ Подключить группу", CALLBACK_GRP + ":begin")))
                .keyboardRow(List.of(button("⬅ Настройки", navPayload("settings"))))
                .build();
    }

    public InlineKeyboardMarkup subscriptionHistoryBackKeyboard() {
        return InlineKeyboardMarkup.builder()
                .keyboardRow(List.of(button("⬅ К подписке", navPayload("subscriptions"))))
                .build();
    }

    private static InlineKeyboardButton button(String text, String callbackData) {
        return InlineKeyboardButton.builder()
                .text(text)
                .callbackData(callbackData)
                .build();
    }
}
