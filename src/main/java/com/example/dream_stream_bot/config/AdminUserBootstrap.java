package com.example.dream_stream_bot.config;

import com.example.dream_stream_bot.config.properties.AdminProperties;
import com.example.dream_stream_bot.model.user.UserEntity;
import com.example.dream_stream_bot.model.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AdminUserBootstrap {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdminUserBootstrap.class);

    @Bean
    public ApplicationRunner bootstrapAdminUser(UserRepository userRepository, AdminProperties adminProperties) {
        AdminProperties.Bootstrap cfg = adminProperties.getBootstrap();
        return args -> {
            if (!cfg.isEnabled()) {
                LOGGER.info("Admin bootstrap disabled");
                return;
            }

            UserEntity adminUser = userRepository.findByTelegramId(cfg.getTelegramId())
                    .orElseGet(() -> new UserEntity(cfg.getTelegramId(), cfg.getUsername(),
                            cfg.getFirstName(), cfg.getLastName()));

            adminUser.setUsername(cfg.getUsername());
            adminUser.setFirstName(cfg.getFirstName());
            adminUser.setLastName(cfg.getLastName());
            adminUser.setRole(UserEntity.UserRole.ADMIN);

            userRepository.save(adminUser);
            LOGGER.info("Admin user bootstrap completed for telegramId={}", cfg.getTelegramId());
        };
    }
}
