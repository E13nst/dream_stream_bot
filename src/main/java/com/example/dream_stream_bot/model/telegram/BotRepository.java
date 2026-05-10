package com.example.dream_stream_bot.model.telegram;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BotRepository extends JpaRepository<BotEntity, Long> {

    /**
     * Loads bot with agent config initialized (needed for cached {@link com.example.dream_stream_bot.service.telegram.BotService#findById} outside a session).
     */
    @Query("SELECT b FROM BotEntity b LEFT JOIN FETCH b.agentConfig WHERE b.id = :id")
    Optional<BotEntity> findWithAgentConfigById(@Param("id") Long id);

    long countByAgentConfig_Id(Long agentConfigId);
} 