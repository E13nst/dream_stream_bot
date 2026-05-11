package com.example.dream_stream_bot.model.subscription;

/** Область тарифа: личное общение или групповой чат. */
public enum TariffScope {
    PERSONAL,
    GROUP;

    public boolean isGroup() {
        return this == GROUP;
    }
}
