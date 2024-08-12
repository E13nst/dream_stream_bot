package com.example.dream_stream_bot.model;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;

public class CommandKeyboardNew {

    InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
    List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
    List<InlineKeyboardButton> row = new ArrayList<>();

    public CommandKeyboardNew addKey(String key, String value) {
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText(key);
        button.setCallbackData(value);

        row.add(button);
        return this;
    }

    public InlineKeyboardMarkup build() {
        keyboard.add(row);
        keyboardMarkup.setKeyboard(keyboard);
        return keyboardMarkup;
    }
}
