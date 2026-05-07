package com.example.dream_stream_bot.security;

import com.example.dream_stream_bot.util.TelegramInitDataValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Фильтр для аутентификации через Telegram initData
 */
@Component
public class TelegramAuthenticationFilter extends OncePerRequestFilter {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(TelegramAuthenticationFilter.class);
    private static final String TELEGRAM_INIT_DATA_HEADER = "X-Telegram-Init-Data";
    private static final String TELEGRAM_BOT_NAME_HEADER = "X-Telegram-Bot-Name";
    private static final String DEFAULT_BOT_NAME = "StickerGallery";
    
    private final TelegramInitDataValidator validator;
    private final TelegramAuthenticationProvider authenticationProvider;
    
    @Autowired
    public TelegramAuthenticationFilter(TelegramInitDataValidator validator, 
                                       TelegramAuthenticationProvider authenticationProvider) {
        this.validator = validator;
        this.authenticationProvider = authenticationProvider;
    }
    
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        
        String initData = request.getHeader(TELEGRAM_INIT_DATA_HEADER);
        String botName = request.getHeader(TELEGRAM_BOT_NAME_HEADER);
        
        // Устанавливаем значение по умолчанию для botName если оно не указано
        boolean botNameWasEmpty = (botName == null || botName.trim().isEmpty());
        if (botNameWasEmpty && initData != null && !initData.trim().isEmpty()) {
            botName = DEFAULT_BOT_NAME;
            LOGGER.info("📝 Заголовок X-Telegram-Bot-Name не указан в запросе к {}. Устанавливаем значение по умолчанию: {}", 
                    request.getRequestURI(), DEFAULT_BOT_NAME);
        }
        
        LOGGER.debug("🔍 TelegramAuthenticationFilter: Запрос к {} | InitData: {} | BotName: {} | DefaultUsed: {}", 
                request.getRequestURI(), 
                initData != null ? "present" : "null", 
                botName != null ? botName : "null",
                botNameWasEmpty && botName != null);
        
        if (initData != null && !initData.trim().isEmpty() && botName != null && !botName.trim().isEmpty()) {
            if (botNameWasEmpty) {
                LOGGER.info("🔍 Обнаружен заголовок X-Telegram-Init-Data. Используется бот по умолчанию: {}", botName);
            } else {
                LOGGER.info("🔍 Обнаружены заголовки X-Telegram-Init-Data и X-Telegram-Bot-Name для бота: {}", botName);
            }
            LOGGER.debug("🔍 InitData (первые 50 символов): {}", 
                    initData.length() > 50 ? initData.substring(0, 50) + "..." : initData);
            
            try {
                // Валидируем initData для конкретного бота
                LOGGER.debug("🔍 Начинаем валидацию initData для бота: {}", botName);
                if (!validator.validateInitData(initData, botName)) {
                    LOGGER.warn("❌ InitData невалидна для бота: {}", botName);
                    filterChain.doFilter(request, response);
                    return;
                }
                LOGGER.debug("✅ InitData валидна для бота: {}", botName);
                
                // Извлекаем telegram_id из initData
                Long telegramId = validator.extractTelegramId(initData);
                LOGGER.debug("🔍 Извлечен telegram_id: {}", telegramId);
                
                if (telegramId != null) {
                    LOGGER.info("🔐 Попытка аутентификации для telegram_id: {} и бота: {}", telegramId, botName);
                    
                    // Создаем неаутентифицированный токен
                    TelegramAuthenticationToken token = new TelegramAuthenticationToken(initData, telegramId, botName);
                    LOGGER.debug("🔍 Создан TelegramAuthenticationToken для telegram_id: {}", telegramId);
                    
                    // Аутентифицируем токен
                    var authentication = authenticationProvider.authenticate(token);
                    LOGGER.debug("🔍 Результат аутентификации: {}", 
                            authentication != null ? authentication.isAuthenticated() : "null");
                    
                    if (authentication != null && authentication.isAuthenticated()) {
                        // Устанавливаем аутентификацию в контекст
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        LOGGER.info("✅ Аутентификация успешна для telegram_id: {} и бота: {}", telegramId, botName);
                    } else {
                        LOGGER.warn("❌ Аутентификация не удалась для telegram_id: {} и бота: {}", telegramId, botName);
                    }
                } else {
                    LOGGER.warn("❌ Не удалось извлечь telegram_id из initData для бота: {}", botName);
                }
                
            } catch (Exception e) {
                LOGGER.error("❌ Ошибка обработки Telegram аутентификации для бота {}: {}", botName, e.getMessage(), e);
            }
        } else {
            LOGGER.debug("🔍 Заголовки Telegram отсутствуют или пусты | InitData: {} | BotName: {}", 
                    initData != null ? "present" : "null", 
                    botName != null ? botName : "null");
        }
        
        // Продолжаем цепочку фильтров
        filterChain.doFilter(request, response);
    }
    
    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        
        // Не фильтруем запросы к статическим ресурсам и некоторым системным эндпоинтам
        return path.startsWith("/actuator/") || 
               path.startsWith("/error") ||
               path.equals("/") ||
               path.startsWith("/webhook/") ||
               path.startsWith("/static/") ||
               path.startsWith("/css/") ||
               path.startsWith("/js/") ||
               path.startsWith("/images/") ||
               path.startsWith("/mini-app/") ||
               path.startsWith("/swagger-ui/") ||
               path.startsWith("/v3/api-docs");
    }
}
