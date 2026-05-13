package com.example.dream_stream_bot.service.telegram;

import com.example.dream_stream_bot.model.telegram.BotEntity;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Locale;
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

    /**
     * Создатель или администратор чата: сначала {@code getChatMember}, при необходимости —
     * {@code getChatAdministrators} (срабатывает, если бот сам админ и ответ getChatMember неоднозначен).
     */
    public boolean isUserChatAdministrator(BotEntity bot, Long chatId, Long telegramUserId) {
        return resolveElevatedMemberStatus(bot, chatId, telegramUserId).isPresent();
    }

    /**
     * Нормализованный статус {@code creator} | {@code administrator} | {@code owner} или пусто.
     */
    public Optional<String> resolveElevatedMemberStatus(BotEntity bot, Long chatId, Long telegramUserId) {
        Optional<String> direct = getChatMemberRole(bot, chatId, telegramUserId);
        boolean directElevated = direct.filter(this::isElevatedAdminRole).isPresent();
        LOGGER.debug("resolveElevatedMemberStatus | chatId={} tgUserId={} | getChatMemberRole={} elevated={}",
                chatId, telegramUserId, direct.orElse("(empty)"), directElevated);
        if (directElevated) {
            return direct;
        }
        Optional<String> fromAdmins = findElevatedStatusInAdministrators(bot, chatId, telegramUserId);
        LOGGER.debug("resolveElevatedMemberStatus | chatId={} tgUserId={} | fromAdministrators={}",
                chatId, telegramUserId, fromAdmins.orElse("(empty)"));
        if (fromAdmins.isEmpty()) {
            LOGGER.warn(
                    "Group admin check: user not elevated | chatId={} tgUserId={} | getChatMemberRole={} (see DEBUG for getChatAdministrators scan)",
                    chatId, telegramUserId, direct.orElse("(empty / mismatch / api error)"));
        }
        return fromAdmins;
    }

    /**
     * Статус участника чата (нормализованный lowercase): {@code creator}, {@code administrator}, {@code member}…
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
                LOGGER.debug("getChatMember empty body chatId={} tgUserId={}", chatId, telegramUserId);
                return Optional.empty();
            }
            JsonNode root = objectMapper.readTree(body);
            boolean ok = root.path("ok").asBoolean(false);
            if (!ok) {
                String desc = root.path("description").asText("");
                LOGGER.warn("getChatMember not ok chatId={} userId={} description={} truncatedResponse={}",
                        chatId, telegramUserId, desc, truncateForLog(body, 400));
                return Optional.empty();
            }
            JsonNode result = root.path("result");
            long returnedUserId = telegramUserIdFromNode(result.path("user"));
            String rawStatus = result.path("status").asText("");
            String status = normalizeStatus(rawStatus);
            LOGGER.debug(
                    "getChatMember parsed | chatId={} expectedTgUserId={} resultUserId={} rawStatus={} normalized={} userIdMatch={}",
                    chatId, telegramUserId, returnedUserId, rawStatus, status, returnedUserId == telegramUserId);
            if (returnedUserId != telegramUserId) {
                LOGGER.warn(
                        "getChatMember user id mismatch | chatId={} expectedTgUserId={} resultUserId={} (normalized status would be {}) | truncatedResponse={}",
                        chatId, telegramUserId, returnedUserId, status, truncateForLog(body, 500));
                return Optional.empty();
            }
            return status.isEmpty() ? Optional.empty() : Optional.of(status);
        } catch (Exception e) {
            LOGGER.warn("getChatMember failed chatId={} tgUserId={}: {}", chatId, telegramUserId, e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<String> findElevatedStatusInAdministrators(BotEntity bot, Long chatId, Long telegramUserId) {
        if (bot == null || bot.getToken() == null || bot.getToken().isBlank()
                || chatId == null || telegramUserId == null) {
            return Optional.empty();
        }
        String url = TELEGRAM_API_URL + bot.getToken()
                + "/getChatAdministrators?chat_id=" + chatId;
        try {
            String body = restTemplate.getForEntity(url, String.class).getBody();
            if (body == null) {
                LOGGER.debug("getChatAdministrators empty body chatId={}", chatId);
                return Optional.empty();
            }
            JsonNode root = objectMapper.readTree(body);
            if (!root.path("ok").asBoolean(false)) {
                String desc = root.path("description").asText("");
                LOGGER.debug("getChatAdministrators not ok chatId={} description={} truncatedResponse={}",
                        chatId, desc, truncateForLog(body, 400));
                return Optional.empty();
            }
            JsonNode arr = root.path("result");
            if (!arr.isArray()) {
                LOGGER.debug("getChatAdministrators result not array chatId={}", chatId);
                return Optional.empty();
            }
            int n = arr.size();
            boolean sawUser = false;
            for (JsonNode m : arr) {
                long uid = telegramUserIdFromNode(m.path("user"));
                if (uid != telegramUserId) {
                    continue;
                }
                sawUser = true;
                String raw = m.path("status").asText("");
                String status = normalizeStatus(raw);
                LOGGER.debug(
                        "getChatAdministrators: matched user | chatId={} tgUserId={} rawStatus={} normalized={} elevated={}",
                        chatId, telegramUserId, raw, status, isElevatedAdminRole(status));
                if (isElevatedAdminRole(status)) {
                    return Optional.of(status);
                }
            }
            LOGGER.debug("getChatAdministrators: no elevated match | chatId={} tgUserId={} adminCount={} sawUserId={}",
                    chatId, telegramUserId, n, sawUser);
        } catch (Exception e) {
            LOGGER.warn("getChatAdministrators failed chatId={}: {}", chatId, e.getMessage());
        }
        return Optional.empty();
    }

    /**
     * Telegram отдаёт {@code user.id} числом; на всякий случай поддерживаем строковое значение.
     */
    private static long telegramUserIdFromNode(JsonNode userNode) {
        if (userNode == null || userNode.isNull() || userNode.isMissingNode()) {
            return 0L;
        }
        JsonNode id = userNode.get("id");
        if (id == null || id.isNull() || id.isMissingNode()) {
            return 0L;
        }
        if (id.isIntegralNumber()) {
            return id.longValue();
        }
        if (id.isTextual()) {
            try {
                return Long.parseLong(id.asText().trim());
            } catch (NumberFormatException e) {
                return 0L;
            }
        }
        if (id.isNumber()) {
            return id.longValue();
        }
        return 0L;
    }

    private static String truncateForLog(String s, int max) {
        if (s == null) {
            return "";
        }
        String t = s.replaceAll("\\s+", " ").trim();
        return t.length() <= max ? t : t.substring(0, max) + "…";
    }

    private static String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return "";
        }
        return status.trim().toLowerCase(Locale.ROOT);
    }

    private boolean isElevatedAdminRole(String normalizedStatus) {
        if (normalizedStatus == null || normalizedStatus.isEmpty()) {
            return false;
        }
        return "creator".equals(normalizedStatus)
                || "administrator".equals(normalizedStatus)
                || "owner".equals(normalizedStatus);
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
                LOGGER.warn("getChat not ok chatId={}: {}", chatId, truncateForLog(body, 400));
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
