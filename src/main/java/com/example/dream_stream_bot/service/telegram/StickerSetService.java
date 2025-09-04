package com.example.dream_stream_bot.service.telegram;

import com.example.dream_stream_bot.model.telegram.StickerSet;
import com.example.dream_stream_bot.model.telegram.StickerSetRepository;
import com.example.dream_stream_bot.service.user.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StickerSetService {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(StickerSetService.class);
    private final StickerSetRepository stickerSetRepository;
    private final UserService userService;
    
    @Autowired
    public StickerSetService(StickerSetRepository stickerSetRepository, UserService userService) {
        this.stickerSetRepository = stickerSetRepository;
        this.userService = userService;
    }
    
    public StickerSet createStickerSet(Long userId, String title, String name) {
        // Автоматически создаем пользователя, если его нет
        try {
            userService.findOrCreateByTelegramId(userId, null, null, null, null);
            LOGGER.info("✅ Пользователь {} автоматически создан/найден при создании стикерпака", userId);
        } catch (Exception e) {
            LOGGER.warn("⚠️ Не удалось создать/найти пользователя {}: {}", userId, e.getMessage());
        }
        
        StickerSet stickerSet = new StickerSet();
        stickerSet.setUserId(userId);
        stickerSet.setTitle(title);
        stickerSet.setName(name);

        StickerSet savedSet = stickerSetRepository.save(stickerSet);
        LOGGER.info("📦 Создан стикерпак: ID={}, Title='{}', Name='{}', UserId={}", 
                savedSet.getId(), title, name, userId);

        return savedSet;
    }

    public StickerSet findByName(String name) {
        return stickerSetRepository.findByName(name).orElse(null);
    }

    public StickerSet findByTitle(String title) {
        return stickerSetRepository.findByTitle(title);
    }

    public List<StickerSet> findByUserId(Long userId) {
        return stickerSetRepository.findByUserId(userId);
    }

    public StickerSet findById(Long id) {
        return stickerSetRepository.findById(id).orElse(null);
    }
    
    public List<StickerSet> findAll() {
        return stickerSetRepository.findAll();
    }
    
    public StickerSet save(StickerSet stickerSet) {
        // Автоматически создаем пользователя, если его нет
        try {
            userService.findOrCreateByTelegramId(stickerSet.getUserId(), null, null, null, null);
            LOGGER.info("✅ Пользователь {} автоматически создан/найден при сохранении стикерпака", stickerSet.getUserId());
        } catch (Exception e) {
            LOGGER.warn("⚠️ Не удалось создать/найти пользователя {}: {}", stickerSet.getUserId(), e.getMessage());
        }
        
        return stickerSetRepository.save(stickerSet);
    }
    
    public void deleteById(Long id) {
        stickerSetRepository.deleteById(id);
    }
} 