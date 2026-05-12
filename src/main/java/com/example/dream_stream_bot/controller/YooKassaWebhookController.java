package com.example.dream_stream_bot.controller;

import com.example.dream_stream_bot.service.payment.SubscriptionPaymentCompletionService;
import com.example.dream_stream_bot.service.payment.YooKassaWebhookIpChecker;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/webhooks")
public class YooKassaWebhookController {

    private static final Logger LOGGER = LoggerFactory.getLogger(YooKassaWebhookController.class);

    private final ObjectMapper objectMapper;
    private final YooKassaWebhookIpChecker ipChecker;
    private final SubscriptionPaymentCompletionService completionService;

    public YooKassaWebhookController(ObjectMapper objectMapper,
                                     YooKassaWebhookIpChecker ipChecker,
                                     SubscriptionPaymentCompletionService completionService) {
        this.objectMapper = objectMapper;
        this.ipChecker = ipChecker;
        this.completionService = completionService;
    }

    @PostMapping(path = "/yookassa", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> receive(HttpServletRequest request, @RequestBody String rawBody) {
        if (!ipChecker.isAllowed(request)) {
            LOGGER.warn("Rejected YooKassa webhook from IP {}", request.getRemoteAddr());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        try {
            JsonNode root = objectMapper.readTree(rawBody);
            String event = root.path("event").asText("");
            if (!"payment.succeeded".equals(event)) {
                return ResponseEntity.ok().build();
            }
            String paymentId = root.path("object").path("id").asText(null);
            if (paymentId != null && !paymentId.isBlank()) {
                completionService.completeByProviderPaymentId(paymentId.trim());
            }
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            LOGGER.error("YooKassa webhook processing failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
