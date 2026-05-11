package com.example.dream_stream_bot.model.keyboard;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.ArrayList;
import java.util.List;

public class ReplyCommandKeyboard {

    private final ReplyKeyboardMarkup keyboard;
    private final List<KeyboardRow> rows;

    public ReplyCommandKeyboard() {
        this.keyboard = new ReplyKeyboardMarkup();
        this.rows = new ArrayList<>();
    }

    public ReplyCommandKeyboard addKey(String key) {
        if (rows.isEmpty() || rows.get(rows.size() - 1).size() >= 2) {
            // Создаем новую строку если текущая пустая или содержит 2 кнопки
            rows.add(new KeyboardRow());
        }
        rows.get(rows.size() - 1).add(new KeyboardButton(key));
        return this;
    }

    /** Следующие {@link #addKey} пойдут в новую строку (текущая строка не дублируется, если уже пустая). */
    public ReplyCommandKeyboard nextRow() {
        if (rows.isEmpty()) {
            rows.add(new KeyboardRow());
            return this;
        }
        KeyboardRow last = rows.get(rows.size() - 1);
        if (!last.isEmpty()) {
            rows.add(new KeyboardRow());
        }
        return this;
    }

    public ReplyKeyboardMarkup build() {
        keyboard.setKeyboard(rows);
        keyboard.setResizeKeyboard(true);
        keyboard.setSelective(false);
        return keyboard;
    }
}
