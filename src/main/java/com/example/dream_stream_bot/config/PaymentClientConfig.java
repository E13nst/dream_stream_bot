package com.example.dream_stream_bot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class PaymentClientConfig {

    private static final String YOOKASSA_API_BASE = "https://api.yookassa.ru/v3";

    @Bean
    public RestClient yookassaRestClient(RestClient.Builder builder) {
        return builder.baseUrl(YOOKASSA_API_BASE).build();
    }
}
