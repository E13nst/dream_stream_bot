package com.example.dream_stream_bot.service.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Клиент к официальному REST API ЮKassa v3 (HTTPS Basic Auth).
 */
@Component
public class YooKassaHttpClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(YooKassaHttpClient.class);

    private final RestClient yookassaRestClient;
    private final ObjectMapper objectMapper;

    public YooKassaHttpClient(RestClient yookassaRestClient, ObjectMapper objectMapper) {
        this.yookassaRestClient = yookassaRestClient;
        this.objectMapper = objectMapper;
    }

    public JsonNode createPayment(YooKassaCredentials credentials, String idempotenceKey, Map<String, Object> body) {
        try {
            String json = objectMapper.writeValueAsString(body);
            String response = yookassaRestClient.post()
                    .uri("/payments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .headers(headers -> {
                        headers.set(HttpHeaders.AUTHORIZATION, basicAuthHeader(credentials));
                        headers.set("Idempotence-Key", idempotenceKey);
                    })
                    .body(json)
                    .retrieve()
                    .body(String.class);
            return objectMapper.readTree(response);
        } catch (RestClientResponseException e) {
            LOGGER.warn("YooKassa createPayment HTTP {}: {}", e.getStatusCode(), e.getResponseBodyAsString(StandardCharsets.UTF_8));
            throw new IllegalStateException("ЮKassa отклонила создание платежа: HTTP " + e.getStatusCode().value(), e);
        } catch (Exception e) {
            throw new IllegalStateException("Ошибка запроса к ЮKassa", e);
        }
    }

    public JsonNode getPayment(YooKassaCredentials credentials, String paymentId) {
        try {
            String response = yookassaRestClient.get()
                    .uri("/payments/{id}", paymentId)
                    .headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, basicAuthHeader(credentials)))
                    .retrieve()
                    .body(String.class);
            return objectMapper.readTree(response);
        } catch (RestClientResponseException e) {
            LOGGER.warn("YooKassa getPayment HTTP {}: {}", e.getStatusCode(), e.getResponseBodyAsString(StandardCharsets.UTF_8));
            throw new IllegalStateException("ЮKassa: не удалось получить платёж", e);
        } catch (Exception e) {
            throw new IllegalStateException("Ошибка запроса к ЮKassa", e);
        }
    }

    private static String basicAuthHeader(YooKassaCredentials c) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(c.shopId(), c.secretKey(), StandardCharsets.UTF_8);
        return headers.getFirst(HttpHeaders.AUTHORIZATION);
    }

    public static String confirmationUrl(JsonNode paymentNode) {
        JsonNode conf = paymentNode.path("confirmation");
        String url = conf.path("confirmation_url").asText(null);
        return url != null && !url.isBlank() ? url : null;
    }

    public static String paymentStatus(JsonNode paymentNode) {
        return paymentNode.path("status").asText(null);
    }

    public static String paymentId(JsonNode paymentNode) {
        return paymentNode.path("id").asText(null);
    }
}
