package com.example.dream_stream_bot.service.telegram;

import com.example.dream_stream_bot.model.telegram.StickerPack;
import com.example.dream_stream_bot.model.telegram.StickerPackRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StickerPackService {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(StickerPackService.class);
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

        StickerPack savedPack = stickerPackRepository.save(stickerPack);

        return savedPack;
    }

    public StickerPack findByName(String name) {
        return stickerPackRepository.findByName(name).orElse(null);
    }

    public StickerPack findByTitle(String title) {
        return stickerPackRepository.findByTitle(title);
    }

    public List<StickerPack> findByUserId(Long userId) {
        return stickerPackRepository.findByUserId(userId);
    }

    public StickerPack findById(Long id) {
        return stickerPackRepository.findById(id).orElse(null);
    }
} 