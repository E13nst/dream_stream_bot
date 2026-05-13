package com.example.dream_stream_bot.service.telegram;

import com.example.dream_stream_bot.model.telegram.BotEntity;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

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
        return getChatMemberRole(bot, chatId, telegramUserId)
                .map(s -> "creator".equals(s) || "administrator".equals(s))
                .orElse(false);
    }

    /**
     * Статус участника чата по Telegram Bot API (например {@code creator}, {@code administrator}, {@code member}).
     */
    public Optional<String> getChatMemberRole(BotEntity bot, Long chatId, Long telegramUserId) {
        if (bot == null || bot.getToken() == null || bot.getToken().isBlank()
                || chatId == null || telegramUserId == null) {
            return Optional.empty();
        }
        String url = TELEGRAM_API_URL + bot.getToken()
                + "/getChatMember?chat_id=" + chatId + "&user_id=" + telegramUserId;
        try {
            String body = restTemplate.getForEntity(url, String.class).getBody();
            if (body == null) {
                return Optional.empty();
            }
            JsonNode root = objectMapper.readTree(body);
            if (!root.path("ok").asBoolean(false)) {
                LOGGER.warn("getChatMember not ok chatId={} userId={}: {}", chatId, telegramUserId, body);
                return Optional.empty();
            }
            String status = root.path("result").path("status").asText("");
            return status.isEmpty() ? Optional.empty() : Optional.of(status);
        } catch (Exception e) {
            LOGGER.warn("getChatMember failed chatId={}: {}", chatId, e.getMessage());
            return Optional.empty();
        }
    }

    /** Заголовок чата (для групп — title) или пусто при ошибке. */
    public Optional<String> getChatTitle(BotEntity bot, Long chatId) {
        if (bot == null || bot.getToken() == null || bot.getToken().isBlank() || chatId == null) {
            return Optional.empty();
        }
        String url = TELEGRAM_API_URL + bot.getToken() + "/getChat?chat_id=" + chatId;
        try {
            String body = restTemplate.getForEntity(url, String.class).getBody();
            if (body == null) {
                return Optional.empty();
            }
            JsonNode root = objectMapper.readTree(body);
            if (!root.path("ok").asBoolean(false)) {
                LOGGER.warn("getChat not ok chatId={}: {}", chatId, body);
                return Optional.empty();
            }
            JsonNode chat = root.path("result");
            String title = chat.path("title").asText("");
            if (!title.isBlank()) {
                return Optional.of(title);
            }
            String username = chat.path("username").asText("");
            if (!username.isBlank()) {
                return Optional.of("@" + username);
            }
            return Optional.empty();
        } catch (Exception e) {
            LOGGER.warn("getChat failed chatId={}: {}", chatId, e.getMessage());
            return Optional.empty();
        }
    }
}
