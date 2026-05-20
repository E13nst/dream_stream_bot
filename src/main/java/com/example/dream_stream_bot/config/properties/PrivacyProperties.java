package com.example.dream_stream_bot.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.privacy")
public class PrivacyProperties {

    /**
     * Salt для SHA-256 хэша telegram_id в журнале удаления. В prod задать через env.
     */
    private String erasureSalt = "dev-change-me-in-production";

    public String getErasureSalt() {
        return erasureSalt;
    }

    public void setErasureSalt(String erasureSalt) {
        this.erasureSalt = erasureSalt;
    }
}
