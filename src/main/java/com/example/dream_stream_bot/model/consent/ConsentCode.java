package com.example.dream_stream_bot.model.consent;

/**
 * Коды документов согласий.
 *
 * <p>OFFER и PRIVACY_POLICY — типичные коды для бота.</p>
 * <p>PERSONAL_DATA, CROSS_BORDER, AGE_18 — сохранены для истории документов; привязка к боту в админке не предлагается.</p>
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
