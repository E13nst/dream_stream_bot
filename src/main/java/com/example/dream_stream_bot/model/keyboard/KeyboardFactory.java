package com.example.dream_stream_bot.model.keyboard;

import com.example.dream_stream_bot.model.dream.DreamState;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

public class KeyboardFactory {

    public static InlineKeyboardMarkup simpleWithCommand(DreamState state, String text) {
        return new InlineKeyboardMarkupBuilder()
                .addKey(String.format("%s \u2705", text), state.name())
                .addKey("Отмена \u274C", DreamState.CANCEL.name())
                .build();
    }
}
