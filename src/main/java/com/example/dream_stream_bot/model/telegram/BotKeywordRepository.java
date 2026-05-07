package com.example.dream_stream_bot.model.telegram;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BotKeywordRepository extends JpaRepository<BotKeywordEntity, Long> {

    boolean existsByBot_IdAndKeywordIgnoreCase(Long botId, String keyword);

    Optional<BotKeywordEntity> findByBot_IdAndKeywordIgnoreCase(Long botId, String keyword);
}
