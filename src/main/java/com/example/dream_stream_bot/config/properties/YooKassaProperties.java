package com.example.dream_stream_bot.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Глобальные настройки ЮKassa (Basic Auth). Поля бота переопределяют shopId/secretKey.
 */
@ConfigurationProperties(prefix = "yookassa")
public class YooKassaProperties {

    private String shopId = "";

    private String secretKey = "";

    /** Шаблон return_url, например {@code https://t.me/{botUsername}} */
    private String returnUrlTemplate = "https://t.me/{botUsername}";

    public String getShopId() {
        return shopId;
    }

    public void setShopId(String shopId) {
        this.shopId = shopId;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getReturnUrlTemplate() {
        return returnUrlTemplate;
    }

    public void setReturnUrlTemplate(String returnUrlTemplate) {
        this.returnUrlTemplate = returnUrlTemplate;
    }

    public String resolveReturnUrl(String botUsername) {
        String u = botUsername == null ? "" : botUsername.trim();
        if (u.startsWith("@")) {
            u = u.substring(1);
        }
        return returnUrlTemplate.replace("{botUsername}", u);
    }
}
