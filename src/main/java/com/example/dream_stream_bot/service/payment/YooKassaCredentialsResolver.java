package com.example.dream_stream_bot.service.payment;

import com.example.dream_stream_bot.config.properties.YooKassaProperties;
import com.example.dream_stream_bot.model.telegram.BotEntity;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class YooKassaCredentialsResolver {

    private final YooKassaProperties properties;

    public YooKassaCredentialsResolver(YooKassaProperties properties) {
        this.properties = properties;
    }

    public Optional<YooKassaCredentials> resolve(BotEntity bot) {
        if (bot != null
                && !isBlank(bot.getYookassaShopId())
                && !isBlank(bot.getYookassaSecretKey())) {
            return Optional.of(new YooKassaCredentials(
                    bot.getYookassaShopId().trim(),
                    bot.getYookassaSecretKey().trim()));
        }
        if (!isBlank(properties.getShopId()) && !isBlank(properties.getSecretKey())) {
            return Optional.of(new YooKassaCredentials(
                    properties.getShopId().trim(),
                    properties.getSecretKey().trim()));
        }
        return Optional.empty();
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
