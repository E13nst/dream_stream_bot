package com.example.dream_stream_bot.model.subscription;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionParticipantRepository extends JpaRepository<SubscriptionParticipantEntity, Long> {

    Optional<SubscriptionParticipantEntity> findBySubscriptionIdAndTelegramId(Long subscriptionId, Long telegramId);

    List<SubscriptionParticipantEntity> findBySubscriptionId(Long subscriptionId);

    @Query("select count(p) from SubscriptionParticipantEntity p " +
           "where p.subscriptionId = :id and p.lastSeenAt >= :since")
    long countActiveSince(@Param("id") Long subscriptionId, @Param("since") OffsetDateTime since);
}
