package com.example.dream_stream_bot.model.subscription;

/** Жизненный цикл подписки. */
public enum SubscriptionStatus {
    /** Создана, но согласия ещё не приняты, и платных/триальных периодов нет. */
    PENDING_CONSENT,
    /** Активна в режиме триала. */
    TRIAL,
    /** Активная оплаченная подписка. */
    ACTIVE,
    /** Срок истёк, новых периодов нет. */
    EXPIRED,
    /** Заблокирована из-за непринятой материальной правки согласий. */
    BLOCKED_CONSENT,
    /** Отменена владельцем или администратором. */
    CANCELLED,
    /**
     * Согласия владельца приняты, но доступ ещё не выдан администратором
     * (типично для групповой подписки без триала).
     */
    AWAITING_ACTIVATION;

    public boolean isAccessAllowed() {
        return this == TRIAL || this == ACTIVE;
    }
}
