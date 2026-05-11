package com.example.dream_stream_bot.model.subscription;

/**
 * Тарифные планы подписки.
 *
 * <ul>
 *   <li>{@link #PERSONAL} — личный план без лимита участников ({@code maxParticipants == null}).</li>
 *   <li>{@link #GROUP_S}/{@link #GROUP_M}/{@link #GROUP_L} — групповые с разным числом участников.</li>
 * </ul>
 */
public enum SubscriptionPlan {
    PERSONAL(null),
    GROUP_S(10),
    GROUP_M(25),
    GROUP_L(50);

    private final Integer defaultMaxParticipants;

    SubscriptionPlan(Integer defaultMaxParticipants) {
        this.defaultMaxParticipants = defaultMaxParticipants;
    }

    public Integer getDefaultMaxParticipants() {
        return defaultMaxParticipants;
    }

    public boolean isGroup() {
        return this != PERSONAL;
    }
}
