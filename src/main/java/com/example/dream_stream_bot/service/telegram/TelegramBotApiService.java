package com.example.dream_stream_bot.service.telegram;

import com.example.dream_stream_bot.model.telegram.BotEntity;
import com.example.dream_stream_bot.service.telegram.BotService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;

import java.util.Optional;

/**
 * Сервис для работы с Telegram Bot API
 */
@Service
public class TelegramBotApiService {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(TelegramBotApiService.class);
    private static final String TELEGRAM_API_URL = "https://api.telegram.org/bot";
    
    private final RestTemplate restTemplate;
    private final BotService botService;
    private final ObjectMapper objectMapper;
    
    @Autowired
    public TelegramBotApiService(BotService botService, ObjectMapper objectMapper) {
        this.restTemplate = new RestTemplate();
        this.botService = botService;
        this.objectMapper = objectMapper;
    }
    
}
