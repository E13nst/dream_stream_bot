package com.example.dream_stream_bot.model.subscription;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionTariffRepository extends JpaRepository<SubscriptionTariffEntity, Long> {

    long countByBotId(Long botId);

    List<SubscriptionTariffEntity> findByBotIdOrderBySortOrderAscIdAsc(Long botId);

    List<SubscriptionTariffEntity> findByBotIdInOrderByBotIdAscSortOrderAsc(Collection<Long> botIds);

    Optional<SubscriptionTariffEntity> findByBotIdAndCode(Long botId, String code);

    Optional<SubscriptionTariffEntity> findByBotIdAndDefaultPersonalTrue(Long botId);

    Optional<SubscriptionTariffEntity> findByBotIdAndDefaultGroupTrue(Long botId);
}
