package com.example.dream_stream_bot.model.subscription;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<SubscriptionEntity, Long> {

    /** Личная подписка пользователя на этом боте (scope_chat_id IS NULL). */
    @Query("select s from SubscriptionEntity s " +
           "where s.botId = :botId and s.ownerUserId = :ownerUserId and s.scopeChatId is null")
    Optional<SubscriptionEntity> findPersonal(@Param("botId") Long botId,
                                              @Param("ownerUserId") Long ownerUserId);

    /** Групповая подписка по чату на этом боте. */
    Optional<SubscriptionEntity> findByBotIdAndScopeChatId(Long botId, Long scopeChatId);

    List<SubscriptionEntity> findByOwnerUserId(Long ownerUserId);

    List<SubscriptionEntity> findByOwnerUserIdAndBotId(Long ownerUserId, Long botId);

    List<SubscriptionEntity> findByBotId(Long botId);

    List<SubscriptionEntity> findByStatus(SubscriptionStatus status);
}
