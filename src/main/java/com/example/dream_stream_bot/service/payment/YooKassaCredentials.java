package com.example.dream_stream_bot.service.payment;

/** Учётные данные Basic Auth для запросов к api.yookassa.ru (конкретный магазин). */
public record YooKassaCredentials(String shopId, String secretKey) {}
