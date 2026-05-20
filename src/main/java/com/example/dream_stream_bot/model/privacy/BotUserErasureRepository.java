package com.example.dream_stream_bot.model.privacy;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BotUserErasureRepository extends JpaRepository<BotUserErasureEntity, Long> {

    boolean existsByBotIdAndTelegramIdHash(Long botId, String telegramIdHash);
}
