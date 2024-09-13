package com.example.dream_stream_bot.model;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;

public class InlineKeyboardMarkupBuilder {

    InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
    List<List<InlineKeyboardButton>> rows = new ArrayList<>();
    List<InlineKeyboardButton> row = new ArrayList<>();

    public InlineKeyboardMarkupBuilder addKey(String key, String value) {
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText(key);
        button.setCallbackData(value);

        row.add(button);
        return this;
    }

    public InlineKeyboardMarkup build() {
        rows.add(row);
        keyboard.setKeyboard(rows);
        return keyboard;
    }
}
