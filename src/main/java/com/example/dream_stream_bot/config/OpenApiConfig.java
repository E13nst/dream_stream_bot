package com.example.dream_stream_bot.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Конфигурация OpenAPI/Swagger
 */
@Configuration
public class OpenApiConfig {
    
    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "TelegramInitData";
        
        return new OpenAPI()
                .info(new Info()
                        .title("Telegram Bot Dream Stream API")
                        .description("""
                                API для Telegram ботов с системой аутентификации через Telegram Web App initData.
                                
                                ## Аутентификация
                                Для доступа к API необходимо использовать аутентификацию через Telegram initData.
                                
                                ### Как получить initData:
                                1. В Telegram Web App вызовите `window.Telegram.WebApp.initData`
                                2. Скопируйте полученную строку
                                3. Вставьте её в поле авторизации в Swagger UI
                                
                                ### Формат initData:
                                ```
                                query_id=...&user=...&auth_date=...&hash=...
                                ```
                                
                                ## Эндпоинты
                                - `/auth/**` - публичные эндпоинты аутентификации
                                - `/api/stickersets/**` - работа со стикерами (требует USER/ADMIN)
                                - `/api/users/**` - управление пользователями (требует USER/ADMIN)
                                - `/api/bots/**` - управление ботами (требует ADMIN)
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Dream Stream Team")
                                .email("support@dreamstream.com")
                                .url("https://dreamstream.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName, new SecurityScheme()
                                .name("X-Telegram-Init-Data")
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .description("""
                                        Telegram Web App initData строка.
                                        
                                        Пример:
                                        ```
                                        query_id=AAHdF6IQAAAAAN0XohDhrOrc&user=%7B%22id%22%3A123456789%2C%22first_name%22%3A%22John%22%2C%22last_name%22%3A%22Doe%22%2C%22username%22%3A%22johndoe%22%2C%22language_code%22%3A%22en%22%7D&auth_date=1640995200&hash=abc123...
                                        ```
                                        
                                        Для тестирования в dev профиле можно использовать тестовый initData из конфигурации.
                                        """)));
    }
}
