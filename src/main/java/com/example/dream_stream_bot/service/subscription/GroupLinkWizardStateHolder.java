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
        /** Показана инструкция, ждём пересылку или deep link. */
        AWAITING_GROUP_FORWARD,
        /** Выбрана группа, ждём inline «Продолжить» / «Другая» / «Отмена». */
        CONFIRMING_GROUP
    }

    public static final class Session {
        private final long tariffId;
        private Step step;
        private Long draftScopeChatId;
        private String draftTitle;
        /** creator | administrator */
        private String draftMemberStatus;

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

        public void setStep(Step step) {
            this.step = step;
        }

        public Long getDraftScopeChatId() {
            return draftScopeChatId;
        }

        public void setDraft(Long scopeChatId, String title, String memberStatus) {
            this.draftScopeChatId = scopeChatId;
            this.draftTitle = title;
            this.draftMemberStatus = memberStatus;
        }

        public String getDraftTitle() {
            return draftTitle;
        }

        public String getDraftMemberStatus() {
            return draftMemberStatus;
        }

        public void clearDraft() {
            this.draftScopeChatId = null;
            this.draftTitle = null;
            this.draftMemberStatus = null;
        }
    }

    private final ConcurrentHashMap<String, Session> sessions = new ConcurrentHashMap<>();

    private static String key(Long botId, Long appUserId) {
        return botId + ":" + appUserId;
    }

    public void startAwaitingForward(Long botId, Long appUserId, long tariffId) {
        if (botId == null || appUserId == null) {
            return;
        }
        sessions.put(key(botId, appUserId), new Session(tariffId, Step.AWAITING_GROUP_FORWARD));
    }

    public void setConfirming(Long botId, Long appUserId, long tariffId, Long scopeChatId, String title,
                              String memberStatus) {
        if (botId == null || appUserId == null) {
            return;
        }
        Session s = new Session(tariffId, Step.CONFIRMING_GROUP);
        s.setDraft(scopeChatId, title, memberStatus);
        sessions.put(key(botId, appUserId), s);
    }

    public Optional<Session> get(Long botId, Long appUserId) {
        if (botId == null || appUserId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(sessions.get(key(botId, appUserId)));
    }

    public void backToAwaitingForward(Long botId, Long appUserId) {
        get(botId, appUserId).ifPresent(s -> {
            if (s.getStep() == Step.CONFIRMING_GROUP) {
                s.setStep(Step.AWAITING_GROUP_FORWARD);
                s.clearDraft();
            }
        });
    }

    public void clear(Long botId, Long appUserId) {
        if (botId == null || appUserId == null) {
            return;
        }
        sessions.remove(key(botId, appUserId));
    }
}
