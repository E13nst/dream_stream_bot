package com.example.dream_stream_bot.model.keyboard;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.ArrayList;
import java.util.List;

public class ReplyCommandKeyboard {

    ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
    List<KeyboardRow> rows = new ArrayList<>();
    KeyboardRow row = new KeyboardRow();

    public ReplyCommandKeyboard addKey(String key) {
        row.add(new KeyboardButton(key));
        return this;
    }

    public ReplyKeyboardMarkup build() {
        rows.add(row);
        keyboard.setKeyboard(rows);
        keyboard.setResizeKeyboard(true);
//        keyboardMarkup.setOneTimeKeyboard(true);
        return keyboard;
    }
}
