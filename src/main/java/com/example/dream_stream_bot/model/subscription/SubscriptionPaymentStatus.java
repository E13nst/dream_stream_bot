package com.example.dream_stream_bot.model.subscription;

/** Статус платежа в нашей БД (не путать со статусом объекта ЮKassa). */
public enum SubscriptionPaymentStatus {
    PENDING,
    SUCCEEDED,
    FAILED,
    CANCELLED
}
