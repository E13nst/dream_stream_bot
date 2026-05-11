package com.example.dream_stream_bot.model.subscription;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReferralBonusGrantRepository extends JpaRepository<ReferralBonusGrantEntity, Long> {

    boolean existsByPaymentPeriodId(Long paymentPeriodId);

    long deleteByTariffId(Long tariffId);
}
