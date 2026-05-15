package com.example.dream_stream_bot.service.payment;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory ожидание email для чека перед созданием платежа ЮKassa (ключ botId:appUserId).
 * При нескольких инстансах приложения сессия не шарится между процессами.
 */
@Component
public class ReceiptEmailAwaitHolder {

    public static final Duration DEFAULT_TTL = Duration.ofMinutes(30);

    public enum Kind {
        PERSONAL_TARIFF,
        GROUP_SUBSCRIPTION
    }

    public static final class Session {
        private final Kind kind;
        private final long personalTariffId;
        private final long groupSubscriptionId;
        private final Instant expiresAt;

        private Session(Kind kind, long personalTariffId, long groupSubscriptionId, Instant expiresAt) {
            this.kind = kind;
            this.personalTariffId = personalTariffId;
            this.groupSubscriptionId = groupSubscriptionId;
            this.expiresAt = expiresAt;
        }

        public static Session personal(long tariffId, Instant expiresAt) {
            return new Session(Kind.PERSONAL_TARIFF, tariffId, 0L, expiresAt);
        }

        public static Session group(long subscriptionId, Instant expiresAt) {
            return new Session(Kind.GROUP_SUBSCRIPTION, 0L, subscriptionId, expiresAt);
        }

        public Kind getKind() {
            return kind;
        }

        public long getPersonalTariffId() {
            return personalTariffId;
        }

        public long getGroupSubscriptionId() {
            return groupSubscriptionId;
        }

        public Instant getExpiresAt() {
            return expiresAt;
        }
    }

    private final Clock clock;
    private final ConcurrentHashMap<String, Session> sessions = new ConcurrentHashMap<>();

    public ReceiptEmailAwaitHolder() {
        this(Clock.systemUTC());
    }

    ReceiptEmailAwaitHolder(Clock clock) {
        this.clock = clock;
    }

    private static String key(Long botId, Long appUserId) {
        return botId + ":" + appUserId;
    }

    public void startAwaitingPersonal(Long botId, Long appUserId, long tariffId) {
        if (botId == null || appUserId == null) {
            return;
        }
        Instant exp = clock.instant().plus(DEFAULT_TTL);
        sessions.put(key(botId, appUserId), Session.personal(tariffId, exp));
    }

    public void startAwaitingGroup(Long botId, Long appUserId, long subscriptionId) {
        if (botId == null || appUserId == null) {
            return;
        }
        Instant exp = clock.instant().plus(DEFAULT_TTL);
        sessions.put(key(botId, appUserId), Session.group(subscriptionId, exp));
    }

    public Optional<Session> get(Long botId, Long appUserId) {
        if (botId == null || appUserId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(sessions.get(key(botId, appUserId)));
    }

    public void clear(Long botId, Long appUserId) {
        if (botId == null || appUserId == null) {
            return;
        }
        sessions.remove(key(botId, appUserId));
    }

    public boolean isExpired(Session session) {
        return session == null || !clock.instant().isBefore(session.getExpiresAt());
    }
}
