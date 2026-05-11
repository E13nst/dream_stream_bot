package com.example.dream_stream_bot.service.access;

import com.example.dream_stream_bot.model.subscription.SubscriptionEntity;

/**
 * Результат проверки {@link AccessGate#evaluate}.
 * Несёт минимум информации, нужный боту для маршрутизации:
 *  - можно ли отдать сообщение в {@code MessageHandlerService};
 *  - нужно ли (и какой текст) показать пользователю как заглушку;
 *  - что показать молча (skip).
 */
public final class AccessDecision {

    private final boolean allowed;
    private final AccessReason reason;
    private final String userMessage;
    private final SubscriptionEntity subscription;

    private AccessDecision(boolean allowed, AccessReason reason, String userMessage, SubscriptionEntity subscription) {
        this.allowed = allowed;
        this.reason = reason;
        this.userMessage = userMessage;
        this.subscription = subscription;
    }

    public static AccessDecision allow(SubscriptionEntity subscription) {
        return new AccessDecision(true, AccessReason.ACTIVE, null, subscription);
    }

    public static AccessDecision allowWithReminder(SubscriptionEntity subscription, AccessReason reason, String reminderText) {
        return new AccessDecision(true, reason, reminderText, subscription);
    }

    public static AccessDecision deny(AccessReason reason, String userMessage) {
        return new AccessDecision(false, reason, userMessage, null);
    }

    public static AccessDecision deny(AccessReason reason) {
        return new AccessDecision(false, reason, null, null);
    }

    public boolean isAllowed() { return allowed; }
    public AccessReason getReason() { return reason; }
    public String getUserMessage() { return userMessage; }
    public SubscriptionEntity getSubscription() { return subscription; }

    /** Если true — заглушку отправлять нужно (если ещё не отправляли в этом окне). */
    public boolean hasUserMessage() {
        return userMessage != null && !userMessage.isBlank();
    }
}
