package com.example.dream_stream_bot.model.consent;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BotConsentBindingRepository extends JpaRepository<BotConsentBindingEntity, Long> {

    List<BotConsentBindingEntity> findByBotIdAndActiveTrueOrderByConsentCodeAsc(Long botId);

    Optional<BotConsentBindingEntity> findFirstByBotIdAndConsentCodeAndActiveTrue(Long botId, ConsentCode consentCode);

    List<BotConsentBindingEntity> findByBotIdAndConsentCodeOrderByCreatedAtDesc(Long botId, ConsentCode consentCode);

    List<BotConsentBindingEntity> findByDocumentIdAndActiveTrue(Long documentId);
}
