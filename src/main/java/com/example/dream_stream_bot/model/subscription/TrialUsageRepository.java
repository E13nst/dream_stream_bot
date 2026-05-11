package com.example.dream_stream_bot.model.subscription;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TrialUsageRepository extends JpaRepository<TrialUsageEntity, Long> {

    Optional<TrialUsageEntity> findByTariffIdAndOwnerUserIdAndScopeChatId(Long tariffId,
                                                                          Long ownerUserId,
                                                                          Long scopeChatId);

    long deleteByTariffId(Long tariffId);
}
