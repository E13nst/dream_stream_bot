package com.example.dream_stream_bot.model.consent;

/**
 * Тип изменения при публикации новой версии документа.
 *
 * <ul>
 *   <li>{@link #MINOR} — техническое уточнение, перепринятие не требуется.</li>
 *   <li>{@link #MATERIAL} — существенное изменение; запускает 14-дневный грейс
 *       на повторное принятие, иначе подписка переходит в {@code BLOCKED_CONSENT}.</li>
 * </ul>
 */
public enum ConsentChangeType {
    MINOR,
    MATERIAL
}
