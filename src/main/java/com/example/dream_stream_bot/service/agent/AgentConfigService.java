package com.example.dream_stream_bot.service.agent;

import com.example.dream_stream_bot.model.agent.AgentConfigEntity;
import com.example.dream_stream_bot.model.agent.AgentConfigRepository;
import com.example.dream_stream_bot.model.agent.AgentProvider;
import com.example.dream_stream_bot.model.agent.AgentRole;
import com.example.dream_stream_bot.model.agent.DataLocality;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class AgentConfigService {

    public static final String CACHE_NAME = "agentConfigs";

    private final AgentConfigRepository agentConfigRepository;
    private final CacheManager cacheManager;

    public AgentConfigService(AgentConfigRepository agentConfigRepository, CacheManager cacheManager) {
        this.agentConfigRepository = agentConfigRepository;
        this.cacheManager = cacheManager;
    }

    @Cacheable(value = CACHE_NAME, key = "#id", unless = "#result == null")
    public AgentConfigEntity findById(Long id) {
        return agentConfigRepository.findById(id).orElse(null);
    }

    public List<AgentConfigEntity> findAll() {
        return agentConfigRepository.findAll();
    }

    public AgentConfigEntity requireById(Long id) {
        return agentConfigRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Agent config not found"));
    }

    @Transactional
    public AgentConfigEntity save(AgentConfigEntity entity) {
        AgentConfigEntity saved = agentConfigRepository.save(entity);
        evictCache(saved.getId());
        return saved;
    }

    @Transactional
    public void deleteById(Long id) {
        agentConfigRepository.deleteById(id);
        evictCache(id);
    }

    /**
     * Load from DB (not cache), update system prompt, save and evict cache.
     */
    @Transactional
    public AgentConfigEntity updateSystemPrompt(Long id, String systemPrompt) {
        return agentConfigRepository.findById(id)
                .map(e -> {
                    e.setSystemPrompt(systemPrompt);
                    return save(e);
                })
                .orElse(null);
    }

    @Transactional
    public AgentConfigEntity update(
            Long id,
            String name,
            String displayName,
            String shortDescription,
            AgentRole role,
            AgentProvider provider,
            DataLocality dataLocality,
            boolean isPublic,
            boolean requireAgeConfirmation,
            String model,
            Double temperature,
            Double topP,
            Double frequencyPenalty,
            Double presencePenalty,
            String systemPrompt,
            Integer memWindow) {
        return agentConfigRepository.findById(id)
                .map(entity -> {
                    entity.setName(name);
                    entity.setDisplayName(displayName);
                    entity.setShortDescription(shortDescription);
                    entity.setRole(role);
                    entity.setProvider(provider);
                    entity.setDataLocality(dataLocality != null ? dataLocality : DataLocality.CROSS_BORDER);
                    entity.setPublic(isPublic);
                    entity.setRequireAgeConfirmation(requireAgeConfirmation);
                    entity.setModel(model);
                    entity.setTemperature(temperature);
                    entity.setTopP(topP);
                    entity.setFrequencyPenalty(frequencyPenalty);
                    entity.setPresencePenalty(presencePenalty);
                    entity.setSystemPrompt(systemPrompt);
                    entity.setMemWindow(memWindow != null ? memWindow : 100);
                    return save(entity);
                })
                .orElse(null);
    }

    private void evictCache(Long id) {
        if (id == null) {
            return;
        }
        var cache = cacheManager.getCache(CACHE_NAME);
        if (cache != null) {
            cache.evict(id);
        }
    }
}
