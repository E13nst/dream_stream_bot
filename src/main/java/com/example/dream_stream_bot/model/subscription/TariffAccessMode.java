package com.example.dream_stream_bot.model.subscription;

/**
 * Режим доступа по тарифу после принятия согласий.
 * {@link #TRIAL_ONBOARDING} — автоматический триал (дни задаются в тарифе).
 */
public enum TariffAccessMode {
    /** Доступ без срока (персонально). */
    FREE_UNLIMITED,
    /** Одноразовый триал с фиксированной длиной после онбординга. */
    TRIAL_ONBOARDING,
    /** Доступ по выданным периодам / ручное продление / ожидание активации. */
    PAID_TERM
}
