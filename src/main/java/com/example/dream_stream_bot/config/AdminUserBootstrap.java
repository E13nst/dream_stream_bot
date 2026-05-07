package com.example.dream_stream_bot.config;

import com.example.dream_stream_bot.model.user.UserEntity;
import com.example.dream_stream_bot.model.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AdminUserBootstrap {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdminUserBootstrap.class);

    @Bean
    public ApplicationRunner bootstrapAdminUser(
            UserRepository userRepository,
            @Value("${admin.bootstrap.enabled:true}") boolean enabled,
            @Value("${admin.bootstrap.telegram-id:1}") Long telegramId,
            @Value("${admin.bootstrap.username:admin}") String username,
            @Value("${admin.bootstrap.first-name:System}") String firstName,
            @Value("${admin.bootstrap.last-name:Admin}") String lastName) {
        return args -> {
            if (!enabled) {
                LOGGER.info("Admin bootstrap disabled");
                return;
            }

            UserEntity adminUser = userRepository.findByTelegramId(telegramId)
                    .orElseGet(() -> {
                        UserEntity created = new UserEntity(telegramId, username, firstName, lastName, null);
                        created.setArtBalance(0L);
                        return created;
                    });

            adminUser.setUsername(username);
            adminUser.setFirstName(firstName);
            adminUser.setLastName(lastName);
            adminUser.setRole(UserEntity.UserRole.ADMIN);

            userRepository.save(adminUser);
            LOGGER.info("Admin user bootstrap completed for telegramId={}", telegramId);
        };
    }
}
