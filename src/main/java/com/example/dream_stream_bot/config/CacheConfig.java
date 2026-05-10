package com.example.dream_stream_bot.config;

import com.example.dream_stream_bot.service.agent.AgentConfigService;
import com.example.dream_stream_bot.service.telegram.BotService;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Separate TTL per cache name: sticker sets, bot rows, agent configs.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager manager = new SimpleCacheManager();
        manager.setCaches(List.of(
                buildCache("stickerSetInfo", 15, TimeUnit.MINUTES, 1000),
                buildCache(BotService.CACHE_NAME, 30, TimeUnit.SECONDS, 10_000),
                buildCache(AgentConfigService.CACHE_NAME, 60, TimeUnit.SECONDS, 10_000)
        ));
        return manager;
    }

    private static CaffeineCache buildCache(String name, long ttl, TimeUnit unit, long maxSize) {
        return new CaffeineCache(name, Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(ttl, unit)
                .recordStats()
                .build());
    }
}
