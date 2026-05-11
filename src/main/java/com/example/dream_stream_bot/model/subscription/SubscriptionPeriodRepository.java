package com.example.dream_stream_bot.model.subscription;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionPeriodRepository extends JpaRepository<SubscriptionPeriodEntity, Long> {

    List<SubscriptionPeriodEntity> findBySubscriptionIdOrderByPeriodEndsAtDesc(Long subscriptionId);
    long countBySubscriptionIdAndSource(Long subscriptionId, PeriodSource source);

    @Query("select max(p.periodEndsAt) from SubscriptionPeriodEntity p where p.subscriptionId = :id")
    Optional<OffsetDateTime> findMaxEndsAt(@Param("id") Long subscriptionId);
}
