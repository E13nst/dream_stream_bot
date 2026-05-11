package com.example.dream_stream_bot.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Типизированный доступ к настройкам админ-пользователя и аварийному оверрайду пароля.
 */
@ConfigurationProperties(prefix = "admin")
public class AdminProperties {

    private Auth auth = new Auth();
    private Bootstrap bootstrap = new Bootstrap();

    public Auth getAuth() { return auth; }
    public void setAuth(Auth auth) { this.auth = auth; }

    public Bootstrap getBootstrap() { return bootstrap; }
    public void setBootstrap(Bootstrap bootstrap) { this.bootstrap = bootstrap; }

    public static class Auth {
        private String username = "admin";
        /** Опциональный аварийный bcrypt-хеш; если задан — затирает запись в БД. */
        private String passwordHash = "";

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getPasswordHash() { return passwordHash; }
        public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    }

    public static class Bootstrap {
        private boolean enabled = true;
        private Long telegramId = 1L;
        private String username = "admin";
        private String firstName = "System";
        private String lastName = "Admin";

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public Long getTelegramId() { return telegramId; }
        public void setTelegramId(Long telegramId) { this.telegramId = telegramId; }

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }

        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }
    }
}
