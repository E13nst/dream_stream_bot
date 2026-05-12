package com.example.dream_stream_bot.model.subscription;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SubscriptionPaymentRepository extends JpaRepository<SubscriptionPaymentEntity, Long> {

    List<SubscriptionPaymentEntity> findTop15ByBotIdAndOwnerUserIdOrderByCreatedAtDesc(Long botId, Long ownerUserId);

    Optional<SubscriptionPaymentEntity> findByProviderPaymentId(String providerPaymentId);

    Optional<SubscriptionPaymentEntity> findByIdAndOwnerUserId(Long id, Long ownerUserId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM SubscriptionPaymentEntity p WHERE p.providerPaymentId = :pid")
    Optional<SubscriptionPaymentEntity> findByProviderPaymentIdForUpdate(@Param("pid") String pid);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM SubscriptionPaymentEntity p WHERE p.id = :id AND p.ownerUserId = :uid")
    Optional<SubscriptionPaymentEntity> findByIdAndOwnerUserIdForUpdate(@Param("id") Long id, @Param("uid") Long uid);
}
