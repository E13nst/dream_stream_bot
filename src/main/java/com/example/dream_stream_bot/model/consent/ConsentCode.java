package com.example.dream_stream_bot.model.consent;

/**
 * Коды документов согласий.
 *
 * <p>OFFER + PRIVACY_POLICY + PERSONAL_DATA + AGE_18 — обязательные для всех пользователей.
 * CROSS_BORDER — требуется только если агент бота имеет {@code data_locality=CROSS_BORDER}.</p>
 */
public enum ConsentCode {
    OFFER("Публичная оферта"),
    PRIVACY_POLICY("Политика обработки персональных данных"),
    PERSONAL_DATA("Согласие на обработку персональных данных"),
    CROSS_BORDER("Согласие на трансграничную передачу"),
    AGE_18("Подтверждение возраста 18+");

    private final String defaultTitle;

    ConsentCode(String defaultTitle) {
        this.defaultTitle = defaultTitle;
    }

    public String getDefaultTitle() {
        return defaultTitle;
    }
}
