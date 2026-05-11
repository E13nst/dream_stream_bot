package com.example.dream_stream_bot.service.telegram;

import com.example.dream_stream_bot.model.telegram.BotEntity;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Вызовы Telegram Bot API, связанные с правами в групповых чатах.
 */
@Service
public class TelegramGroupAdminService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TelegramGroupAdminService.class);

    private static final String TELEGRAM_API_URL = "https://api.telegram.org/bot";

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper;

    public TelegramGroupAdminService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /** Проверяет, есть ли пользователь среди администраторов чата (creator | administrator). */
    public boolean isUserChatAdministrator(BotEntity bot, Long chatId, Long telegramUserId) {
        if (bot == null || bot.getToken() == null || bot.getToken().isBlank()) {
            return false;
        }
        if (chatId == null || telegramUserId == null) {
            return false;
        }
        String url = TELEGRAM_API_URL + bot.getToken()
                + "/getChatAdministrators?chat_id=" + chatId;
        try {
            String body = restTemplate.getForEntity(url, String.class).getBody();
            if (body == null) {
                return false;
            }
            JsonNode root = objectMapper.readTree(body);
            if (!root.path("ok").asBoolean(false)) {
                LOGGER.warn("getChatAdministrators not ok chatId={}: {}", chatId, body);
                return false;
            }
            JsonNode result = root.path("result");
            if (!result.isArray()) {
                return false;
            }
            for (JsonNode m : result) {
                JsonNode user = m.path("user");
                if (telegramUserId.equals(user.path("id").asLong(Long.MIN_VALUE))) {
                    String status = m.path("status").asText("");
                    return "creator".equals(status) || "administrator".equals(status);
                }
            }
            return false;
        } catch (Exception e) {
            LOGGER.warn("getChatAdministrators failed chatId={}: {}", chatId, e.getMessage());
            return false;
        }
    }
}
