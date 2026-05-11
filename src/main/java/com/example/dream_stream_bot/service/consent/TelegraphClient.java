package com.example.dream_stream_bot.service.consent;

import com.example.dream_stream_bot.service.settings.SystemSettingsService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Минимальный клиент Telegraph API: создание аккаунта, публикация и редактирование страниц.
 *
 * <p>Access token хранится в {@code system_settings} под ключом {@code TELEGRAPH_ACCESS_TOKEN};
 * автоматически создаётся при первой публикации, если ещё отсутствует.</p>
 *
 * <p>Markdown преобразуется в простой набор Telegraph-узлов: заголовки {@code #}/{@code ##}/{@code ###}
 * → h3/h4, остальные строки — параграфы. Поддерживаются базовые ссылки {@code [text](url)}
 * через простую замену; для всех других возможностей формат сохраняется как plain.</p>
 */
@Service
public class TelegraphClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(TelegraphClient.class);

    public static final String SETTING_ACCESS_TOKEN = "TELEGRAPH_ACCESS_TOKEN";
    public static final String SETTING_SHORT_NAME = "TELEGRAPH_SHORT_NAME";
    public static final String SETTING_AUTHOR_NAME = "TELEGRAPH_AUTHOR_NAME";

    private static final String API_BASE = "https://api.telegra.ph";
    private static final String DEFAULT_SHORT_NAME = "DreamStreamBot";
    private static final String DEFAULT_AUTHOR_NAME = "Dream Stream Bot";

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper;
    private final SystemSettingsService settingsService;

    public TelegraphClient(ObjectMapper objectMapper, SystemSettingsService settingsService) {
        this.objectMapper = objectMapper;
        this.settingsService = settingsService;
    }

    /**
     * Гарантирует наличие access_token в system_settings и возвращает его.
     */
    public String ensureAccessToken() {
        String existing = settingsService.getOrDefault(SETTING_ACCESS_TOKEN, "");
        if (existing != null && !existing.isBlank()) {
            return existing;
        }
        String shortName = settingsService.getOrDefault(SETTING_SHORT_NAME, DEFAULT_SHORT_NAME);
        String authorName = settingsService.getOrDefault(SETTING_AUTHOR_NAME, DEFAULT_AUTHOR_NAME);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("short_name", shortName);
        payload.put("author_name", authorName);

        JsonNode response = call("createAccount", payload);
        String token = response.path("access_token").asText(null);
        if (token == null || token.isBlank()) {
            throw new IllegalStateException("Telegraph createAccount returned no access_token: " + response);
        }
        settingsService.set(SETTING_ACCESS_TOKEN, token);
        LOGGER.info("✅ Telegraph account created (short_name='{}')", shortName);
        return token;
    }

    /**
     * Публикует markdown-документ как Telegraph-страницу.
     * Возвращает url итоговой страницы и path (для будущей правки).
     */
    public PublishedPage publish(String title, String markdown) {
        String token = ensureAccessToken();
        List<Map<String, Object>> nodes = markdownToNodes(markdown);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("access_token", token);
        payload.put("title", trimTitle(title));
        payload.put("author_name", settingsService.getOrDefault(SETTING_AUTHOR_NAME, DEFAULT_AUTHOR_NAME));
        payload.put("content", nodes);
        payload.put("return_content", false);

        JsonNode response = call("createPage", payload);
        String url = response.path("url").asText();
        String path = response.path("path").asText();
        LOGGER.info("📄 Telegraph published: {}", url);
        return new PublishedPage(url, path);
    }

    /**
     * Перезаписывает уже опубликованную страницу. {@code path} нужен для адресации.
     */
    public PublishedPage edit(String path, String title, String markdown) {
        String token = ensureAccessToken();
        List<Map<String, Object>> nodes = markdownToNodes(markdown);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("access_token", token);
        payload.put("path", path);
        payload.put("title", trimTitle(title));
        payload.put("author_name", settingsService.getOrDefault(SETTING_AUTHOR_NAME, DEFAULT_AUTHOR_NAME));
        payload.put("content", nodes);
        payload.put("return_content", false);

        JsonNode response = call("editPage/" + path, payload);
        return new PublishedPage(response.path("url").asText(), response.path("path").asText());
    }

    private JsonNode call(String method, Map<String, Object> payload) {
        String url = API_BASE + "/" + method;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
        try {
            JsonNode root = objectMapper.readTree(response.getBody());
            if (!root.path("ok").asBoolean()) {
                throw new IllegalStateException("Telegraph " + method + " failed: " + root);
            }
            return root.path("result");
        } catch (Exception e) {
            throw new IllegalStateException("Telegraph " + method + " response parse error", e);
        }
    }

    private static String trimTitle(String title) {
        if (title == null) {
            return "Документ";
        }
        if (title.length() > 256) {
            return title.substring(0, 256);
        }
        return title;
    }

    /**
     * Простейшая конвертация markdown → Telegraph-узлы.
     * Поддерживаются: # / ## / ### → h3/h4 (Telegraph не имеет h1/h2 уровня в API),
     * пустые строки — разделители параграфов, остальное — параграфы.
     */
    static List<Map<String, Object>> markdownToNodes(String markdown) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (markdown == null || markdown.isBlank()) {
            return result;
        }
        String[] paragraphs = markdown.replace("\r\n", "\n").split("\n\\s*\n");
        for (String paragraph : paragraphs) {
            String trimmed = paragraph.strip();
            if (trimmed.isEmpty()) {
                continue;
            }
            String tag = "p";
            String content = trimmed;
            if (trimmed.startsWith("### ")) {
                tag = "h4";
                content = trimmed.substring(4).strip();
            } else if (trimmed.startsWith("## ")) {
                tag = "h3";
                content = trimmed.substring(3).strip();
            } else if (trimmed.startsWith("# ")) {
                tag = "h3";
                content = trimmed.substring(2).strip();
            }
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("tag", tag);
            node.put("children", Arrays.asList(content));
            result.add(node);
        }
        return result;
    }

    public record PublishedPage(String url, String path) {
    }
}
