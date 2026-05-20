package com.example.dream_stream_bot.model.subscription;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ReferralBonusGrantRepository extends JpaRepository<ReferralBonusGrantEntity, Long> {

    boolean existsByPaymentPeriodId(Long paymentPeriodId);

    long deleteByTariffId(Long tariffId);

    @Modifying
    @Query("delete from ReferralBonusGrantEntity g where g.botId = :botId "
            + "and (g.referredUserId = :userId or g.referrerUserId = :userId)")
    int deleteByBotIdAndUserId(@Param("botId") Long botId, @Param("userId") Long userId);
}
