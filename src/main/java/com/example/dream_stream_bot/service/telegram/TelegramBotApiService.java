package com.example.dream_stream_bot.service.telegram;

import com.example.dream_stream_bot.model.telegram.BotEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Сервис для работы с Telegram Bot API
 */
@Service
public class TelegramBotApiService {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(TelegramBotApiService.class);
    private static final String TELEGRAM_API_URL = "https://api.telegram.org/bot";
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    @Autowired
    public TelegramBotApiService(ObjectMapper objectMapper) {
        this.restTemplate = new RestTemplate();
        this.objectMapper = objectMapper;
    }

    public record WebhookInfo(
            boolean ok,
            String url,
            Integer pendingUpdateCount,
            String lastErrorMessage
    ) {
        public boolean isWebhookEnabled() {
            return url != null && !url.isBlank();
        }

        public String describeDelivery() {
            if (!ok) return "unknown(telegram_api_error)";
            if (isWebhookEnabled()) return "webhook(url=" + url + ")";
            return "long-polling(no webhook)";
        }
    }

    public Optional<WebhookInfo> getWebhookInfo(BotEntity bot) {
        String token = bot.getToken();
        if (token == null || token.isBlank()) {
            LOGGER.warn("❌ getWebhookInfo skipped: token is empty for bot '{}'", bot.getUsername());
            return Optional.empty();
        }

        String url = TELEGRAM_API_URL + token + "/getWebhookInfo";

        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            if (response == null || response.getBody() == null) return Optional.empty();

            var json = objectMapper.readTree(response.getBody());
            boolean ok = json.has("ok") && json.get("ok").asBoolean(false);

            String webhookUrl = null;
            Integer pending = null;
            String lastError = null;
            if (json.has("result") && json.get("result") != null && json.get("result").isObject()) {
                var result = json.get("result");
                if (result.has("url")) webhookUrl = result.get("url").asText(null);
                if (result.has("pending_update_count")) pending = result.get("pending_update_count").isNull() ? null : result.get("pending_update_count").asInt();
                if (result.has("last_error_message")) lastError = result.get("last_error_message").asText(null);
            }

            return Optional.of(new WebhookInfo(ok, webhookUrl, pending, lastError));
        } catch (Exception e) {
            LOGGER.warn("❌ Failed to get webhook info for bot '{}': {}", bot.getUsername(), e.getMessage());
            return Optional.empty();
        }
    }
    
    public boolean setWebhook(BotEntity bot, String webhookUrl, String secretToken) {
        String token = bot.getToken();
        if (token == null || token.isBlank()) {
            LOGGER.warn("❌ setWebhook skipped: token is empty for bot '{}'", bot.getUsername());
            return false;
        }
        if (webhookUrl == null || webhookUrl.isBlank()) {
            LOGGER.warn("❌ setWebhook skipped: webhookUrl is empty for bot '{}'", bot.getUsername());
            return false;
        }

        String url = TELEGRAM_API_URL + token + "/setWebhook";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> payload = new HashMap<>();
        payload.put("url", webhookUrl);
        if (secretToken != null && !secretToken.isBlank()) {
            payload.put("secret_token", secretToken);
        }

        try {
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            boolean ok = isTelegramOk(response);
            if (ok) {
                LOGGER.info("✅ Webhook set for bot '{}' -> {}", bot.getUsername(), webhookUrl);
            } else {
                LOGGER.error("❌ Failed to set webhook for bot '{}' -> {} | response: {}",
                        bot.getUsername(), webhookUrl, safeBody(response));
            }
            return ok;
        } catch (RestClientException e) {
            LOGGER.error("❌ Failed to set webhook for bot '{}' -> {}: {}",
                    bot.getUsername(), webhookUrl, e.getMessage(), e);
            return false;
        }
    }

    public boolean deleteWebhook(BotEntity bot) {
        String token = bot.getToken();
        if (token == null || token.isBlank()) {
            LOGGER.warn("❌ deleteWebhook skipped: token is empty for bot '{}'", bot.getUsername());
            return false;
        }

        String url = TELEGRAM_API_URL + token + "/deleteWebhook";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> payload = new HashMap<>();
        payload.put("drop_pending_updates", true);

        try {
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            boolean ok = isTelegramOk(response);
            if (ok) {
                LOGGER.info("✅ Webhook deleted for bot '{}'", bot.getUsername());
            } else {
                LOGGER.error("❌ Failed to delete webhook for bot '{}' | response: {}",
                        bot.getUsername(), safeBody(response));
            }
            return ok;
        } catch (RestClientException e) {
            LOGGER.error("❌ Failed to delete webhook for bot '{}': {}", bot.getUsername(), e.getMessage(), e);
            return false;
        }
    }

    /**
     * Отправка произвольного сообщения через {@code sendMessage} (чат = личка или id группы).
     */
    public boolean sendTextMessage(BotEntity bot, long chatId, String text, Map<String, Object> replyMarkup) {
        String token = bot.getToken();
        if (token == null || token.isBlank()) {
            LOGGER.warn("❌ sendTextMessage skipped: empty token for bot '{}'", bot.getUsername());
            return false;
        }
        if (text == null || text.isBlank()) {
            return false;
        }
        String url = TELEGRAM_API_URL + token + "/sendMessage";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> payload = new HashMap<>();
        payload.put("chat_id", chatId);
        payload.put("text", text);
        if (replyMarkup != null && !replyMarkup.isEmpty()) {
            payload.put("reply_markup", replyMarkup);
        }
        try {
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            boolean ok = isTelegramOk(response);
            if (!ok) {
                LOGGER.warn("❌ sendTextMessage failed for bot '{}' chatId={} | {}", bot.getUsername(), chatId, safeBody(response));
            }
            return ok;
        } catch (RestClientException e) {
            LOGGER.warn("❌ sendTextMessage exception bot '{}' chatId={}: {}", bot.getUsername(), chatId, e.getMessage());
            return false;
        }
    }

    /**
     * Описание команды для меню Telegram.
     * Поле {@code command} — имя без слэша; {@code description} — 1..256 символов.
     */
    public record MenuCommand(String command, String description) {}

    /**
     * Устанавливает меню команд бота через {@code setMyCommands}.
     *
     * @param bot          бот, чьим токеном пользуемся
     * @param commands     список команд (передача пустого списка очищает меню для этого scope)
     * @param scope        опциональный {@code BotCommandScope}-объект,
     *                     например {@code Map.of("type", "all_private_chats")}.
     *                     {@code null} — применить как default-меню.
     * @param languageCode опциональный IETF-код (например, {@code "ru"}). {@code null} — без локали.
     */
    public boolean setMyCommands(BotEntity bot,
                                 List<MenuCommand> commands,
                                 Map<String, Object> scope,
                                 String languageCode) {
        String token = bot.getToken();
        if (token == null || token.isBlank()) {
            LOGGER.warn("❌ setMyCommands skipped: empty token for bot '{}'", bot.getUsername());
            return false;
        }
        if (commands == null) {
            commands = List.of();
        }

        String url = TELEGRAM_API_URL + token + "/setMyCommands";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> payload = new HashMap<>();
        payload.put("commands", commands.stream()
                .map(c -> Map.of("command", c.command(), "description", c.description()))
                .toList());
        if (scope != null && !scope.isEmpty()) {
            payload.put("scope", scope);
        }
        if (languageCode != null && !languageCode.isBlank()) {
            payload.put("language_code", languageCode);
        }

        try {
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            boolean ok = isTelegramOk(response);
            if (ok) {
                LOGGER.info("✅ setMyCommands ok for bot '{}' scope={} lang={} count={}",
                        bot.getUsername(), scope, languageCode, commands.size());
            } else {
                LOGGER.warn("❌ setMyCommands failed for bot '{}' scope={} lang={} | {}",
                        bot.getUsername(), scope, languageCode, safeBody(response));
            }
            return ok;
        } catch (RestClientException e) {
            LOGGER.warn("❌ setMyCommands exception bot '{}' scope={} lang={}: {}",
                    bot.getUsername(), scope, languageCode, e.getMessage());
            return false;
        }
    }

    private boolean isTelegramOk(ResponseEntity<String> response) {
        if (response == null || response.getBody() == null) return false;
        try {
            var json = objectMapper.readTree(response.getBody());
            return json.has("ok") && json.get("ok").asBoolean(false);
        } catch (Exception e) {
            return false;
        }
    }

    private String safeBody(ResponseEntity<String> response) {
        if (response == null) return "null";
        return response.getBody() != null ? response.getBody() : "<empty>";
    }
}
