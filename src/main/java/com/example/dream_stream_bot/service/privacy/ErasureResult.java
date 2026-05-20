package com.example.dream_stream_bot.service.privacy;

/**
 * Итог {@link UserDataErasureService#eraseForBot(long, long, long)}.
 */
public record ErasureResult(
        boolean alreadyErased,
        int subscriptionsDeleted,
        int participantsRemoved,
        int consentsDeleted,
        int referralGrantsDeleted,
        int chatMemoryRowsDeleted
) {
}
