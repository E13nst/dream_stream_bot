package com.example.dream_stream_bot.service.privacy;

import com.example.dream_stream_bot.config.properties.PrivacyProperties;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Component
public class TelegramIdHashService {

    private final String erasureSalt;

    public TelegramIdHashService(PrivacyProperties privacyProperties) {
        this.erasureSalt = privacyProperties.getErasureSalt();
    }

    public String hashForBot(long botId, long telegramUserId) {
        String payload = botId + ":" + telegramUserId + ":" + erasureSalt;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
