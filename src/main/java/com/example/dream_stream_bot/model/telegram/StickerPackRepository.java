package com.example.dream_stream_bot.model.telegram;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StickerPackRepository extends JpaRepository<StickerPack, Long> {
    
    List<StickerPack> findByUserId(Long userId);
    
    Optional<StickerPack> findByName(String name);
    
    StickerPack findByTitle(String title);
} 