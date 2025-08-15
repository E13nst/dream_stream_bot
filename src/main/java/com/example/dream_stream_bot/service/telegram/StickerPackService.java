package com.example.dream_stream_bot.service.telegram;

import com.example.dream_stream_bot.model.telegram.StickerPack;
import com.example.dream_stream_bot.model.telegram.StickerPackRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class StickerPackService {
    
    private final StickerPackRepository stickerPackRepository;
    
    @Autowired
    public StickerPackService(StickerPackRepository stickerPackRepository) {
        this.stickerPackRepository = stickerPackRepository;
    }
    
    public StickerPack createStickerPack(Long userId, String title, String name) {
        StickerPack stickerPack = new StickerPack();
        stickerPack.setUserId(userId);
        stickerPack.setTitle(title);
        stickerPack.setName(name);
        return stickerPackRepository.save(stickerPack);
    }
    
    public StickerPack findByName(String name) {
        return stickerPackRepository.findByName(name).orElse(null);
    }
    
    public StickerPack findByTitle(String title) {
        return stickerPackRepository.findByTitle(title);
    }
    
    public java.util.List<StickerPack> findByUserId(Long userId) {
        return stickerPackRepository.findByUserId(userId);
    }
    
    public StickerPack findById(Long id) {
        return stickerPackRepository.findById(id).orElse(null);
    }
} 