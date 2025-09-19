package com.example.dream_stream_bot.config;

import io.lettuce.core.RedisConnectionException;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.SslOptions;
import java.time.Duration;

/**
 * Конфигурация Redis для кэширования стикеров
 */
@Configuration
public class RedisConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisConfig.class);

    @Value("${spring.data.redis.host:redis-e13nst.amvera.io}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    @Value("${spring.data.redis.database:0}")
    private int redisDatabase;

    /**
     * Настройка подключения к Redis
     */
    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {
        LOGGER.info("🔧 Настройка Redis подключения: host={}, port={}, database={}", redisHost, redisPort, redisDatabase);
        
        RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration();
        configuration.setHostName(redisHost);
        configuration.setPort(redisPort);
        configuration.setDatabase(redisDatabase);
        
        if (redisPassword != null && !redisPassword.trim().isEmpty()) {
            configuration.setPassword(redisPassword);
            LOGGER.info("🔐 Redis пароль установлен");
        } else {
            LOGGER.info("🔓 Redis пароль не установлен");
        }
        
        // Настройка SSL для Lettuce (как показал Python тест)
        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                .commandTimeout(Duration.ofSeconds(10))
                .useSsl()  // Включаем SSL
                .build();
        
        LOGGER.info("🔒 SSL включен для Redis подключения");
        
        LettuceConnectionFactory factory = new LettuceConnectionFactory(configuration, clientConfig);
        // Настройка таймаутов для graceful degradation
        factory.setValidateConnection(false);
        
        LOGGER.info("🏭 LettuceConnectionFactory создан с SSL");
        
        return factory;
    }

    /**
     * Настройка Redis Template для работы со стикерами
     */
    @Bean(name = "stickerRedisTemplate")
    public RedisTemplate<String, Object> stickerRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // Сериализация ключей как строки
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        
        // Сериализация значений как JSON
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        
        template.afterPropertiesSet();
        return template;
    }
}