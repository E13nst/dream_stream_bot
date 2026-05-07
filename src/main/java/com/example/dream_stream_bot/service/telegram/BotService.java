package com.example.dream_stream_bot.service.telegram;

import com.example.dream_stream_bot.model.telegram.BotEntity;
import com.example.dream_stream_bot.model.telegram.BotKeywordEntity;
import com.example.dream_stream_bot.model.telegram.BotKeywordRepository;
import com.example.dream_stream_bot.model.telegram.BotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Service
public class BotService {
    private final BotRepository botRepository;
    private final BotKeywordRepository botKeywordRepository;

    @Autowired
    public BotService(BotRepository botRepository, BotKeywordRepository botKeywordRepository) {
        this.botRepository = botRepository;
        this.botKeywordRepository = botKeywordRepository;
    }

    public List<BotEntity> getAllBots() {
        return botRepository.findAll();
    }

    public List<BotEntity> findAll() {
        return botRepository.findAll();
    }

    public BotEntity findById(Long id) {
        return botRepository.findById(id).orElse(null);
    }

    public List<BotEntity> findByType(String type) {
        return botRepository.findAll().stream()
                .filter(bot -> bot.getType().equalsIgnoreCase(type))
                .toList();
    }

    public List<BotEntity> findActiveBots() {
        return botRepository.findAll().stream()
                .filter(bot -> Boolean.TRUE.equals(bot.getIsActive()))
                .toList();
    }

    /**
     * Сохранение бота с автоматическим обновлением updatedAt
     */
    public BotEntity save(BotEntity bot) {
        if (bot.getId() == null) {
            // Новый бот - устанавливаем createdAt
            bot.setCreatedAt(LocalDateTime.now());
        }
        // Обновляем updatedAt для всех сохранений
        bot.setUpdatedAt(LocalDateTime.now());
        return botRepository.save(bot);
    }

    public void deleteById(Long id) {
        botRepository.deleteById(id);
    }

    /**
     * Проверка существования бота по ID
     */
    public boolean existsById(Long id) {
        return botRepository.existsById(id);
    }

    /**
     * Проверка существования бота по username
     */
    public boolean existsByUsername(String username) {
        return botRepository.findAll().stream()
                .anyMatch(bot -> username.equalsIgnoreCase(bot.getUsername()));
    }

    /**
     * Поиск бота по имени
     */
    public Optional<BotEntity> findByName(String name) {
        return botRepository.findAll().stream()
                .filter(bot -> name.equals(bot.getName()))
                .findFirst();
    }

    /**
     * Поиск бота по username
     */
    public Optional<BotEntity> findByUsername(String username) {
        return botRepository.findAll().stream()
                .filter(bot -> username.equalsIgnoreCase(bot.getUsername()))
                .findFirst();
    }

    /**
     * Добавить ключевое слово-триггер. Дубликат по регистру — 409.
     */
    @Transactional
    public BotEntity addKeyword(Long botId, String rawKeyword) {
        BotEntity bot = botRepository.findById(botId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Бот не найден"));
        String keyword = normalizeKeyword(rawKeyword);
        if (keyword.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ключевое слово не может быть пустым");
        }
        if (botKeywordRepository.existsByBot_IdAndKeywordIgnoreCase(botId, keyword)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Такое ключевое слово уже есть у этого бота");
        }
        BotKeywordEntity row = new BotKeywordEntity();
        row.setBot(bot);
        row.setKeyword(keyword);
        bot.getKeywords().add(row);
        return save(bot);
    }

    /**
     * Удалить ключевое слово (сравнение без учёта регистра).
     */
    @Transactional
    public BotEntity removeKeyword(Long botId, String rawKeyword) {
        BotEntity bot = botRepository.findById(botId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Бот не найден"));
        String keyword = normalizeKeyword(rawKeyword);
        if (keyword.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ключевое слово не может быть пустым");
        }
        Optional<BotKeywordEntity> existing = botKeywordRepository.findByBot_IdAndKeywordIgnoreCase(botId, keyword);
        if (existing.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Ключевое слово не найдено");
        }
        bot.getKeywords().remove(existing.get());
        return save(bot);
    }

    /**
     * Полная замена списка ключевых слов (null = не менять; вызывать только с non-null из контроллера).
     */
    @Transactional
    public BotEntity replaceKeywords(Long botId, List<String> rawList) {
        BotEntity bot = botRepository.findById(botId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Бот не найден"));
        bot.getKeywords().clear();
        if (rawList == null || rawList.isEmpty()) {
            return save(bot);
        }
        Set<String> seenLower = new HashSet<>();
        for (String raw : rawList) {
            String kw = normalizeKeyword(raw);
            if (kw.isEmpty()) {
                continue;
            }
            String lower = kw.toLowerCase(Locale.ROOT);
            if (!seenLower.add(lower)) {
                continue;
            }
            BotKeywordEntity row = new BotKeywordEntity();
            row.setBot(bot);
            row.setKeyword(kw);
            bot.getKeywords().add(row);
        }
        return save(bot);
    }

    private static String normalizeKeyword(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim();
    }
}
