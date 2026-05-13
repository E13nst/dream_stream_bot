package com.example.dream_stream_bot.service.subscription;

import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory сессия мастера привязки группы (ключ botId:appUserId).
 */
@Component
public class GroupLinkWizardStateHolder {

    public enum Step {
        /** Показана кнопка startgroup, ждём выбор группы в Telegram. */
        AWAITING_GROUP_PICK,
        /** Выбрана группа, ждём inline «Продолжить» / «Другая» / «Отмена». */
        CONFIRMING_GROUP
    }

    public static final class Session {
        private final long tariffId;
        private Step step;
        private Long draftScopeChatId;

        public Session(long tariffId, Step step) {
            this.tariffId = tariffId;
            this.step = step;
        }

        public long getTariffId() {
            return tariffId;
        }

        public Step getStep() {
            return step;
        }

        public Long getDraftScopeChatId() {
            return draftScopeChatId;
        }

        public void setDraftScopeChatId(Long scopeChatId) {
            this.draftScopeChatId = scopeChatId;
        }
    }

    private final ConcurrentHashMap<String, Session> sessions = new ConcurrentHashMap<>();

    private static String key(Long botId, Long appUserId) {
        return botId + ":" + appUserId;
    }

    public void startAwaitingGroupPick(Long botId, Long appUserId, long tariffId) {
        if (botId == null || appUserId == null) {
            return;
        }
        sessions.put(key(botId, appUserId), new Session(tariffId, Step.AWAITING_GROUP_PICK));
    }

    public void setConfirming(Long botId, Long appUserId, long tariffId, Long scopeChatId) {
        if (botId == null || appUserId == null) {
            return;
        }
        Session s = new Session(tariffId, Step.CONFIRMING_GROUP);
        s.setDraftScopeChatId(scopeChatId);
        sessions.put(key(botId, appUserId), s);
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
}
