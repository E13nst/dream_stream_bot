package com.example.dream_stream_bot.service.telegram;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.File;

/**
 * Сервис для работы с Telegram Bot API
 */
@Service
public class TelegramApiService {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(TelegramApiService.class);
    
    private final RestTemplate restTemplate = new RestTemplate();
    private String botToken;
    
    public void setBotToken(String botToken) {
        this.botToken = botToken;
    }
    
    /**
     * Проверяет информацию о боте
     */
    public boolean checkBotInfo() {
        try {
            String url = apiUrl("getMe");
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            
            if (response.getStatusCode().is2xxSuccessful() && 
                response.getBody() != null && 
                response.getBody().contains("\"ok\":true")) {
                LOGGER.info("✅ Бот успешно прошел проверку getMe");
                return true;
            }
            
            LOGGER.error("❌ Неуспешная проверка бота: Status={}, Body={}", 
                    response.getStatusCode(), response.getBody());
            return false;
            
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при проверке информации о боте: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Формирует URL для API метода
     */
    private String apiUrl(String method) {
        return "https://api.telegram.org/bot" + botToken + "/" + method + "?";
    }
}