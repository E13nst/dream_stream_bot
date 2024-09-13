package com.example.dream_stream_bot.model;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

public class KeyboardFactory {

    public static InlineKeyboardMarkup simpleWithCommand(String text, DreamCommand command) {
        return new InlineKeyboardMarkupBuilder()
                .addKey(String.format("%s \u2705", text), command.name())
                .addKey("Отмена \u274C", DreamCommand.CANCEL.name())
                .build();
    }
}
