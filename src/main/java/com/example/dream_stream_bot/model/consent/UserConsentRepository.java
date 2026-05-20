package com.example.dream_stream_bot.model.consent;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserConsentRepository extends JpaRepository<UserConsentEntity, Long> {

    List<UserConsentEntity> findByUserId(Long userId);

    Optional<UserConsentEntity> findFirstByUserIdAndDocumentIdAndRevokedAtIsNull(Long userId, Long documentId);

    @Modifying
    @Query("update UserConsentEntity uc set uc.revokedAt = :now where uc.userId = :userId and uc.revokedAt is null")
    int revokeAllForUser(@Param("userId") Long userId, @Param("now") OffsetDateTime now);

    @Modifying
    @Query("update UserConsentEntity uc set uc.revokedAt = :now where uc.userId = :userId "
            + "and uc.revokedAt is null "
            + "and uc.subscriptionId in (select s.id from SubscriptionEntity s where s.botId = :botId)")
    int revokeLinkedToSubscriptionsOnBot(@Param("userId") Long userId,
                                           @Param("botId") Long botId,
                                           @Param("now") OffsetDateTime now);

    @Modifying
    @Query("delete from UserConsentEntity uc where uc.userId = :userId "
            + "and uc.documentId in (select b.documentId from BotConsentBindingEntity b where b.botId = :botId)")
    int deleteForUserOnBot(@Param("userId") Long userId, @Param("botId") Long botId);
}
