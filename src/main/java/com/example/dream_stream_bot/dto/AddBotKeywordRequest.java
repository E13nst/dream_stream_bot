package com.example.dream_stream_bot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Тело запроса на добавление ключевого слова-триггера бота.
 */
public class AddBotKeywordRequest {

    @NotBlank(message = "Ключевое слово обязательно")
    @Size(max = 256, message = "Ключевое слово не должно превышать 256 символов")
    private String keyword;

    public AddBotKeywordRequest() {
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }
}
