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
    public static final String BTN_DREAM = "\uD83C\uDF19 Рассказать сон";
    public static final String BTN_DIARY = "\uD83D\uDCD6 Мой дневник";
    public static final String BTN_SETTINGS = "⚙\uFE0F Настройки";
    public static final String BTN_SUBSCRIPTION = "\uD83D\uDC8E Подписка";

    public static final String CALLBACK_NAV = "nav";

    public ReplyKeyboard privateMainKeyboard() {
        return new ReplyCommandKeyboard()
                .addKey(BTN_START)
                .nextRow()
                .addKey(BTN_DREAM)
                .addKey(BTN_DIARY)
                .addKey(BTN_SETTINGS)
                .addKey(BTN_SUBSCRIPTION)
                .build();
    }

    public InlineKeyboardMarkup privateSettingsInlineKeyboard() {
        return InlineKeyboardMarkup.builder()
                .keyboardRow(List.of(button("💎 Моя подписка", navPayload("subscriptions"))))
                .keyboardRow(List.of(button("\uD83D\uDD17 Пригласить друга", navPayload("referral"))))
                .keyboardRow(List.of(
                        button("\uD83D\uDDD1 Забыть последний обмен", navPayload("forget_last")),
                        button("\uD83D\uDEAA Удалить аккаунт", navPayload("forget_me"))))
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

    private static InlineKeyboardButton button(String text, String callbackData) {
        return InlineKeyboardButton.builder()
                .text(text)
                .callbackData(callbackData)
                .build();
    }
}
