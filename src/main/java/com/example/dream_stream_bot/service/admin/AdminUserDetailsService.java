package com.example.dream_stream_bot.service.admin;

import com.example.dream_stream_bot.model.settings.SystemSetting;
import com.example.dream_stream_bot.model.settings.SystemSettingsRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * DB-backed UserDetailsService для веб-админки.
 *
 * Единственный источник истины — таблица system_settings:
 *   - {@link #PASSWORD_HASH_KEY}   — BCrypt-хеш текущего пароля
 *   - {@link #IS_DEFAULT_KEY}      — "true", пока пароль не был сменён вручную
 *
 * При первом старте (записей в БД нет) сидирует дефолтный пароль "admin".
 * Опциональный аварийный оверрайд: env ADMIN_AUTH_PASSWORD_HASH —
 * если задан, используется вместо дефолта и сбрасывает флаг is_default.
 */
@Service
public class AdminUserDetailsService implements UserDetailsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdminUserDetailsService.class);

    static final String PASSWORD_HASH_KEY = "admin_password_hash";
    static final String IS_DEFAULT_KEY    = "admin_password_is_default";

    private static final String DEFAULT_PASSWORD = "admin";

    private final SystemSettingsRepository settingsRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminUsername;
    private final String emergencyPasswordHash;

    public AdminUserDetailsService(
            SystemSettingsRepository settingsRepository,
            PasswordEncoder passwordEncoder,
            @Value("${admin.auth.username:admin}") String adminUsername,
            @Value("${admin.auth.password-hash:}") String emergencyPasswordHash) {
        this.settingsRepository = settingsRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminUsername = adminUsername;
        this.emergencyPasswordHash = emergencyPasswordHash;
    }

    @PostConstruct
    @Transactional
    public void seedIfAbsent() {
        if (emergencyPasswordHash != null && !emergencyPasswordHash.isBlank()) {
            settingsRepository.save(new SystemSetting(PASSWORD_HASH_KEY, emergencyPasswordHash));
            settingsRepository.save(new SystemSetting(IS_DEFAULT_KEY, "false"));
            LOGGER.warn("Admin password overridden via ADMIN_AUTH_PASSWORD_HASH");
            return;
        }

        if (settingsRepository.existsById(PASSWORD_HASH_KEY)) {
            LOGGER.info("Admin password hash already present in DB — skipping seed");
            return;
        }

        settingsRepository.save(new SystemSetting(PASSWORD_HASH_KEY, passwordEncoder.encode(DEFAULT_PASSWORD)));
        settingsRepository.save(new SystemSetting(IS_DEFAULT_KEY, "true"));
        LOGGER.warn("Admin seeded with default password '{}' — change it after first login", DEFAULT_PASSWORD);
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if (!adminUsername.equals(username)) {
            throw new UsernameNotFoundException("Admin user not found: " + username);
        }

        String hash = settingsRepository.findById(PASSWORD_HASH_KEY)
                .map(SystemSetting::getValue)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Admin password hash not found in DB. Restart the app to trigger seeding."));

        return User.withUsername(adminUsername)
                .password(hash)
                .roles("ADMIN")
                .build();
    }

    /**
     * Смена пароля с проверкой текущего.
     *
     * @throws IllegalArgumentException если текущий пароль неверен
     */
    @Transactional
    public void changePassword(String currentPassword, String newPassword) {
        UserDetails admin = loadUserByUsername(adminUsername);
        if (!passwordEncoder.matches(currentPassword, admin.getPassword())) {
            throw new IllegalArgumentException("Неверный текущий пароль");
        }
        settingsRepository.save(new SystemSetting(PASSWORD_HASH_KEY, passwordEncoder.encode(newPassword)));
        settingsRepository.save(new SystemSetting(IS_DEFAULT_KEY, "false"));
        LOGGER.info("Admin password changed successfully");
    }

    public boolean isDefaultPassword() {
        return settingsRepository.findById(IS_DEFAULT_KEY)
                .map(s -> "true".equals(s.getValue()))
                .orElse(false);
    }
}
