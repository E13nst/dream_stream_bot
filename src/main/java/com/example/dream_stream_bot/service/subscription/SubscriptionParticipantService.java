package com.example.dream_stream_bot.service.subscription;

import com.example.dream_stream_bot.model.subscription.SubscriptionParticipantEntity;
import com.example.dream_stream_bot.model.subscription.SubscriptionParticipantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
public class SubscriptionParticipantService {

    private final SubscriptionParticipantRepository participantRepository;

    public SubscriptionParticipantService(SubscriptionParticipantRepository participantRepository) {
        this.participantRepository = participantRepository;
    }

    /** Уникальные активные участники группы за период (last_seen привязка к месяцу). */
    public long countActiveSince(Long subscriptionId, OffsetDateTime since) {
        if (subscriptionId == null || since == null) {
            return 0;
        }
        return participantRepository.countActiveSince(subscriptionId, since);
    }

    @Transactional
    public void touch(Long subscriptionId, Long telegramUserId) {
        if (subscriptionId == null || telegramUserId == null) {
            return;
        }
        OffsetDateTime now = OffsetDateTime.now();
        SubscriptionParticipantEntity p = participantRepository
                .findBySubscriptionIdAndTelegramId(subscriptionId, telegramUserId)
                .orElseGet(() -> {
                    SubscriptionParticipantEntity entity = new SubscriptionParticipantEntity();
                    entity.setSubscriptionId(subscriptionId);
                    entity.setTelegramId(telegramUserId);
                    entity.setFirstSeenAt(now);
                    entity.setLastSeenAt(now);
                    return entity;
                });
        p.setLastSeenAt(now);
        participantRepository.save(p);
    }
}
