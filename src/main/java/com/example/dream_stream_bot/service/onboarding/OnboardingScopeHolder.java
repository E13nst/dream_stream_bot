package com.example.dream_stream_bot.service.onboarding;

import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Временное хранение контекста онбординга (групповой чат), пока владелец проходит
 * согласия в личке. Ключ: {@code botId:appUserId}.
 */
@Component
public class OnboardingScopeHolder {

    private final ConcurrentHashMap<String, Long> pendingGroupChatId = new ConcurrentHashMap<>();

    /** group_consent_<subId> — onboarding участника группы в личке. */
    private final ConcurrentHashMap<String, Long> pendingParticipantSubscriptionId = new ConcurrentHashMap<>();

    private static String key(Long botId, Long appUserId) {
        return botId + ":" + appUserId;
    }

    public void setPendingGroupChat(Long botId, Long appUserId, Long telegramChatId) {
        if (botId == null || appUserId == null || telegramChatId == null) {
            return;
        }
        pendingGroupChatId.put(key(botId, appUserId), telegramChatId);
    }

    public Optional<Long> getPendingGroupChat(Long botId, Long appUserId) {
        if (botId == null || appUserId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(pendingGroupChatId.get(key(botId, appUserId)));
    }

    public void clearPendingGroup(Long botId, Long appUserId) {
        if (botId == null || appUserId == null) {
            return;
        }
        pendingGroupChatId.remove(key(botId, appUserId));
    }

    public void setPendingParticipantSubscription(Long botId, Long appUserId, Long subscriptionId) {
        if (botId == null || appUserId == null || subscriptionId == null) {
            return;
        }
        pendingParticipantSubscriptionId.put(key(botId, appUserId), subscriptionId);
    }

    public Optional<Long> getPendingParticipantSubscription(Long botId, Long appUserId) {
        if (botId == null || appUserId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(pendingParticipantSubscriptionId.get(key(botId, appUserId)));
    }

    public void clearPendingParticipant(Long botId, Long appUserId) {
        if (botId == null || appUserId == null) {
            return;
        }
        pendingParticipantSubscriptionId.remove(key(botId, appUserId));
    }
}