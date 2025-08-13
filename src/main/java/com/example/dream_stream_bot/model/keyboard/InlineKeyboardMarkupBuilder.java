package com.example.dream_stream_bot.model.keyboard;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;

public class InlineKeyboardMarkupBuilder {

    private final InlineKeyboardMarkup keyboard;
    private final List<List<InlineKeyboardButton>> rows;

    public InlineKeyboardMarkupBuilder() {
        this.keyboard = new InlineKeyboardMarkup();
        this.rows = new ArrayList<>();
    }

    public InlineKeyboardMarkupBuilder addKey(String text, String callbackData) {
        if (rows.isEmpty()) {
            rows.add(new ArrayList<>());
        }
        
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText(text);
        button.setCallbackData(callbackData);
        
        rows.get(rows.size() - 1).add(button);
        return this;
    }

    public InlineKeyboardMarkupBuilder addRow(String... buttons) {
        List<InlineKeyboardButton> row = new ArrayList<>();
        for (String buttonText : buttons) {
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(buttonText);
            button.setCallbackData(buttonText.toLowerCase().replace(" ", "_"));
            row.add(button);
        }
        rows.add(row);
        return this;
    }

    public InlineKeyboardMarkup build() {
        keyboard.setKeyboard(rows);
        return keyboard;
    }
}
