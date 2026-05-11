package com.example.dream_stream_bot.service.telegram;

import com.example.dream_stream_bot.model.agent.AgentConfigEntity;
import com.example.dream_stream_bot.model.telegram.BotEntity;
import com.example.dream_stream_bot.model.telegram.BotKeywordEntity;
import com.example.dream_stream_bot.model.telegram.BotKeywordRepository;
import com.example.dream_stream_bot.model.telegram.BotRepository;
import com.example.dream_stream_bot.service.agent.AgentConfigService;
import com.example.dream_stream_bot.service.subscription.SubscriptionTariffService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
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

    public static final String CACHE_NAME = "bots";

    private final BotRepository botRepository;
    private final BotKeywordRepository botKeywordRepository;
    private final AgentConfigService agentConfigService;
    private final CacheManager cacheManager;
    private final SubscriptionTariffService subscriptionTariffService;

    @Autowired
    public BotService(BotRepository botRepository,
                      BotKeywordRepository botKeywordRepository,
                      AgentConfigService agentConfigService,
                      CacheManager cacheManager,
                      SubscriptionTariffService subscriptionTariffService) {
        this.botRepository = botRepository;
        this.botKeywordRepository = botKeywordRepository;
        this.agentConfigService = agentConfigService;
        this.cacheManager = cacheManager;
        this.subscriptionTariffService = subscriptionTariffService;
    }

    public List<BotEntity> getAllBots() {
        return botRepository.findAll();
    }

    public List<BotEntity> findAll() {
        return botRepository.findAll();
    }

    @Cacheable(value = CACHE_NAME, key = "#id", unless = "#result == null")
    public BotEntity findById(Long id) {
        return botRepository.findWithAgentConfigById(id).orElse(null);
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

    @Transactional
    public BotEntity save(BotEntity bot) {
        boolean isNew = bot.getId() == null;
        if (isNew) {
            bot.setCreatedAt(LocalDateTime.now());
        }
        bot.setUpdatedAt(LocalDateTime.now());
        assertAssistantHasAgent(bot);
        BotEntity saved = botRepository.save(bot);
        if (isNew) {
            subscriptionTariffService.ensureDefaultTariffsForBot(saved.getId());
        }
        evictBotCache(saved.getId());
        return saved;
    }

    private static void assertAssistantHasAgent(BotEntity bot) {
        if ("assistant".equalsIgnoreCase(bot.getType()) && bot.getAgentConfig() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Assistant bot must reference an agent (agent_config_id)");
        }
    }

    /**
     * Удаляет только строку бота; общий {@link AgentConfigEntity} не удаляется.
     */
    @Transactional
    public void deleteById(Long id) {
        botRepository.findById(id).ifPresent(bot -> {
            botRepository.delete(bot);
            evictBotCache(id);
        });
    }

    private void evictBotCache(Long id) {
        if (id == null) {
            return;
        }
        var cache = cacheManager.getCache(CACHE_NAME);
        if (cache != null) {
            cache.evict(id);
        }
    }

    public boolean existsById(Long id) {
        return botRepository.existsById(id);
    }

    public boolean existsByUsername(String username) {
        return botRepository.findAll().stream()
                .anyMatch(bot -> username.equalsIgnoreCase(bot.getUsername()));
    }

    public Optional<BotEntity> findByName(String name) {
        return botRepository.findAll().stream()
                .filter(bot -> name.equals(bot.getName()))
                .findFirst();
    }

    public Optional<BotEntity> findByUsername(String username) {
        return botRepository.findAll().stream()
                .filter(bot -> username.equalsIgnoreCase(bot.getUsername()))
                .findFirst();
    }

    /** Сколько ботов ссылается на этот agent_config (для админки). */
    public long countBotsUsingAgent(Long agentConfigId) {
        if (agentConfigId == null) {
            return 0;
        }
        return botRepository.countByAgentConfig_Id(agentConfigId);
    }

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
