package com.example.dream_stream_bot.bot;

import com.example.dream_stream_bot.model.telegram.BotEntity;
import com.example.dream_stream_bot.service.telegram.MessageHandlerService;
import com.example.dream_stream_bot.service.telegram.StickerService;

import com.example.dream_stream_bot.service.telegram.UserStateService;
import com.example.dream_stream_bot.service.telegram.StickerPackService;
import com.example.dream_stream_bot.model.keyboard.InlineKeyboardMarkupBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.springframework.beans.factory.annotation.Autowired;
import com.example.dream_stream_bot.model.telegram.StickerPack;

import java.util.List;

public class StickerBot extends AbstractTelegramBot {
    
    private final StickerService stickerService;
    private static final Logger LOGGER = LoggerFactory.getLogger(StickerBot.class);
    
    private final UserStateService userStateService;
    private final StickerPackService stickerPackService;
    
    public StickerBot(BotEntity botEntity, MessageHandlerService messageHandlerService, 
                     UserStateService userStateService, StickerPackService stickerPackService,
                     StickerService stickerService) {
        super(botEntity, messageHandlerService);
        this.stickerService = stickerService;
        this.userStateService = userStateService;
        this.stickerPackService = stickerPackService;
    }

    @Override
    public void onUpdateReceived(Update update) {
        // Добавляем логирование всех входящих сообщений
        if (update.hasMessage()) {
            Message msg = update.getMessage();
            LOGGER.info("📨 Получено сообщение | ChatId: {} | Тип: {} | HasPhoto: {} | HasDocument: {} | HasText: {} | HasSticker: {} | HasVideo: {} | HasAudio: {} | Text: '{}'", 
                    msg.getChatId(),
                    msg.hasPhoto() ? "PHOTO" : msg.hasDocument() ? "DOCUMENT" : msg.hasText() ? "TEXT" : msg.hasSticker() ? "STICKER" : msg.hasVideo() ? "VIDEO" : msg.hasAudio() ? "AUDIO" : "OTHER",
                    msg.hasPhoto(),
                    msg.hasDocument(),
                    msg.hasText(),
                    msg.hasSticker(),
                    msg.hasVideo(),
                    msg.hasAudio(),
                    msg.hasText() ? msg.getText() : "N/A");
        }
        
        // Обработка callback-ов от inline кнопок
        if (update.hasCallbackQuery()) {
            String callbackData = update.getCallbackQuery().getData();
            Long chatId = update.getCallbackQuery().getMessage().getChatId();
            LOGGER.info("🔘 Получен callback | ChatId: {} | Data: {}", chatId, callbackData);
            
            if ("создать_новый_набор".equals(callbackData)) {
                // Начинаем процесс создания стикерпака
                userStateService.setUserState(chatId, UserStateService.UserState.WAITING_FOR_PACK_TITLE);
                
                SendMessage infoMessage = SendMessage.builder()
                        .chatId(chatId)
                        .text("Введи имя для нового стикерпака:")
                        .build();
                sendWithLogging(infoMessage);
                return;
            } else if ("помощь".equals(callbackData)) {
                SendMessage helpMessage = SendMessage.builder()
                        .chatId(chatId)
                        .text("🎯 **StickerBot** - создание стикеров из изображений\n\n" +
                                "📸 **Как использовать:**\n" +
                                "• Отправьте фото или изображение (JPG, PNG, GIF)\n" +
                                "• Бот автоматически создаст стикер\n" +
                                "• Готовый стикер будет отправлен в чат\n\n" +
                                "⚠️ **Требования к изображению:**\n" +
                                "• Формат: PNG, WebP\n" +
                                "• Размер: до 512KB\n" +
                                "• Разрешение: квадратное (512x512 пикселей)\n\n" +
                                "🚀 **Начните прямо сейчас** - отправьте изображение!")
                        .parseMode("Markdown")
                        .build();
                sendWithLogging(helpMessage);
                return;
            } else if ("информация".equals(callbackData)) {
                SendMessage infoMessage = SendMessage.builder()
                        .chatId(chatId)
                        .text("ℹ️ **Информация о StickerBot**\n\n" +
                                "🤖 **Версия:** 1.0\n" +
                                "📅 **Дата:** 2024\n" +
                                "🔧 **Технологии:** Spring Boot, Telegram Bot API\n\n" +
                                "📱 **Возможности:**\n" +
                                "• Автоматическое создание стикеров\n" +
                                "• Оптимизация изображений\n" +
                                "• Сохранение пропорций\n" +
                                "• Поддержка PNG/WebP\n\n" +
                                "💡 **Для создания стикера отправьте изображение!**")
                        .parseMode("Markdown")
                        .build();
                sendWithLogging(infoMessage);
                return;
            }
        }
        
        if (update.hasMessage()) {
            Message msg = update.getMessage();
            String conversationId = getConversationId(msg.getChatId());
            
            // Проверяем, нужно ли отвечать (для групповых чатов)
            boolean isGroup = msg.isGroupMessage() || msg.isSuperGroupMessage();
            if (isGroup) {
                boolean isReplyToBot = msg.getReplyToMessage() != null &&
                        msg.getReplyToMessage().getFrom() != null &&
                        msg.getReplyToMessage().getFrom().getUserName() != null &&
                        msg.getReplyToMessage().getFrom().getUserName().equalsIgnoreCase(getBotUsername());
                boolean isMention = msg.hasText() && msg.getText().toLowerCase().contains("@" + getBotUsername().toLowerCase());
                boolean isName = msg.hasText() && msg.getText().toLowerCase().contains(botEntity.getName().toLowerCase());
                boolean isAlias = msg.hasText() && botEntity.getBotTriggersList().stream().anyMatch(alias -> !alias.isEmpty() && msg.getText().toLowerCase().contains(alias.toLowerCase()));
                boolean isTrigger = msg.hasText() && botEntity.getBotTriggersList().stream().anyMatch(trigger -> !trigger.isEmpty() && msg.getText().toLowerCase().contains(trigger.toLowerCase()));
                
                if (!(isReplyToBot || isMention || isName || isAlias || isTrigger)) {
                    return; // Игнорируем сообщение, если не обращение к боту
                }
            }
            
            // Обрабатываем изображения
            if (msg.hasPhoto() || msg.hasDocument()) {
                LOGGER.info("🖼️ Обрабатываем изображение | ChatId: {} | Тип: {}", msg.getChatId(), msg.hasPhoto() ? "PHOTO" : "DOCUMENT");
                
                // Получаем name стикерпака из базы данных для этого пользователя
                String stickerPackName = null;
                try {
                    List<StickerPack> userPacks = stickerPackService.findByUserId(msg.getChatId());
                    if (!userPacks.isEmpty()) {
                        // Берем последний созданный стикерпак (самый новый)
                        StickerPack latestPack = userPacks.stream()
                                .max((p1, p2) -> p1.getCreatedAt().compareTo(p2.getCreatedAt()))
                                .orElse(userPacks.get(0));
                        stickerPackName = latestPack.getName();
                        LOGGER.info("📦 Найден стикерпак для пользователя {}: Name='{}', Created={}", 
                                msg.getChatId(), stickerPackName, latestPack.getCreatedAt());
                    } else {
                        LOGGER.warn("⚠️ Стикерпак для пользователя {} не найден в базе данных", msg.getChatId());
                    }
                } catch (Exception e) {
                    LOGGER.error("❌ Ошибка при поиске стикерпака для пользователя {}: {}", msg.getChatId(), e.getMessage());
                }
                
                SendMessage response = stickerService.handleImageMessage(msg, this, stickerPackName);
                if (response != null) {
                    sendWithLogging(response);
                }
                // Если response == null, значит стикер уже был отправлен в StickerService
            } else if (msg.hasText()) {
                // Проверяем состояние пользователя
                UserStateService.UserState currentState = userStateService.getUserState(msg.getChatId());
                
                if (currentState == UserStateService.UserState.WAITING_FOR_PACK_TITLE) {
                    // Пользователь ввел название стикерпака
                    String title = msg.getText();
                    if (title.length() > 64) {
                        SendMessage errorMessage = SendMessage.builder()
                                .chatId(msg.getChatId())
                                .text("❌ Название слишком длинное! Максимум 64 символа. Попробуйте еще раз:")
                                .build();
                        sendWithLogging(errorMessage);
                        return;
                    }
                    
                    // Сохраняем название и переходим к следующему шагу
                    UserStateService.StickerPackData packData = new UserStateService.StickerPackData();
                    packData.setTitle(title);
                    userStateService.setStickerPackData(msg.getChatId(), packData);
                    userStateService.setUserState(msg.getChatId(), UserStateService.UserState.WAITING_FOR_PACK_NAME);
                    
                    SendMessage nextStepMessage = SendMessage.builder()
                            .chatId(msg.getChatId())
                            .text("Введите имя ссылки для нового пакета стикеров:\n\n" +
                                    "💡 Подсказка: окончание '_by_StickerGalleryBot' добавится автоматически\n" +
                                    "Пример: если введете 'my_stickers', получится 'my_stickers_by_StickerGalleryBot'")
                            .build();
                    sendWithLogging(nextStepMessage);
                    return;
                    
                } else if (currentState == UserStateService.UserState.WAITING_FOR_PACK_NAME) {
                    // Пользователь ввел имя ссылки стикерпака
                    String userInput = msg.getText();
                    
                    // Автоматически добавляем корректное окончание
                    String name = userInput;
                    if (!name.endsWith("_by_StickerGalleryBot")) {
                        name = userInput + "_by_StickerGalleryBot";
                    }
                    
                    // Проверяем уникальность имени
                    StickerPack existingPack = stickerPackService.findByName(name);
                    if (existingPack != null) {
                        SendMessage errorMessage = SendMessage.builder()
                                .chatId(msg.getChatId())
                                .text("❌ Стикерпак с таким именем уже существует!\n\n" +
                                        "Попробуйте другое имя ссылки:")
                                .build();
                        sendWithLogging(errorMessage);
                        return;
                    }
                    
                    // Сохраняем имя и завершаем создание
                    UserStateService.StickerPackData packData = userStateService.getStickerPackData(msg.getChatId());
                    packData.setName(name);
                    
                    // Сохраняем в базу данных
                    StickerPack savedPack = stickerPackService.createStickerPack(
                        msg.getChatId(), packData.getTitle(), packData.getName());
                    LOGGER.info("📦 Создан стикерпак: Title='{}', Name='{}', UserId={}, DB_ID={}", 
                            packData.getTitle(), packData.getName(), msg.getChatId(), savedPack.getId());
                    
                    // Очищаем состояние пользователя
                    userStateService.clearUserState(msg.getChatId());
                    
                    SendMessage successMessage = SendMessage.builder()
                            .chatId(msg.getChatId())
                            .text("✅ Стикерпак создан!\n\n" +
                                    "📝 Название: " + packData.getTitle() + "\n" +
                                    "🔗 Имя: " + packData.getName() + "\n\n" +
                                    "Теперь отправьте изображение для создания стикера!")
                            .build();
                    sendWithLogging(successMessage);
                    return;
                }
                
                // Обрабатываем текстовые сообщения
                String text = msg.getText().toLowerCase();
                
                // Обработка команды /start
                if (text.equals("/start")) {
                    InlineKeyboardMarkup keyboard = new InlineKeyboardMarkupBuilder()
                            .addRow("Создать новый набор", "Помощь", "Информация")
                            .build();
                    
                    SendMessage startMessage = SendMessage.builder()
                            .chatId(msg.getChatId())
                            .text("🎯 **Добро пожаловать в StickerBot!**\n\n" +
                                    "Я помогу вам создавать стикеры из изображений.\n\n" +
                                    "📸 **Как использовать:**\n" +
                                    "• Нажмите кнопку 'Создать новый набор' или\n" +
                                    "• Отправьте фото или изображение (JPG, PNG, GIF)\n" +
                                    "• Бот автоматически создаст стикер\n" +
                                    "• Готовый стикер будет отправлен в чат\n\n" +
                                    "🚀 **Начните прямо сейчас!**")
                            .parseMode("Markdown")
                            .replyMarkup(keyboard)
                            .build();
                    sendWithLogging(startMessage);
                    return;
                }
                
                // Обработка кнопки "Создать новый набор"
                if (text.equals("создать новый набор")) {
                    SendMessage infoMessage = SendMessage.builder()
                            .chatId(msg.getChatId())
                            .text("📸 **Отправьте изображение для создания стикера!**\n\n" +
                                    "Поддерживаемые форматы: JPG, PNG, GIF\n" +
                                    "Для справки напишите: помощь")
                            .parseMode("Markdown")
                            .build();
                    sendWithLogging(infoMessage);
                    return;
                }
                
                // Обработка кнопки "Помощь"
                if (text.equals("помощь")) {
                    SendMessage helpMessage = SendMessage.builder()
                            .chatId(msg.getChatId())
                            .text("🎯 **StickerBot** - создание стикеров из изображений\n\n" +
                                    "📸 **Как использовать:**\n" +
                                    "• Отправьте фото или изображение (JPG, PNG, GIF)\n" +
                                    "• Бот автоматически создаст стикер\n" +
                                    "• Готовый стикер будет отправлен в чат\n\n" +
                                    "⚠️ **Требования к изображению:**\n" +
                                    "• Формат: PNG, WebP\n" +
                                    "• Размер: до 512KB\n" +
                                    "• Разрешение: квадратное (512x512 пикселей)\n\n" +
                                    "🚀 **Начните прямо сейчас** - отправьте изображение!")
                            .parseMode("Markdown")
                            .build();
                    sendWithLogging(helpMessage);
                    return;
                }
                
                // Обработка кнопки "Информация"
                if (text.equals("информация")) {
                    SendMessage infoMessage = SendMessage.builder()
                            .chatId(msg.getChatId())
                            .text("ℹ️ **Информация о StickerBot**\n\n" +
                                    "🤖 **Версия:** 1.0\n" +
                                    "📅 **Дата:** 2024\n" +
                                    "🔧 **Технологии:** Spring Boot, Telegram Bot API\n\n" +
                                    "📱 **Возможности:**\n" +
                                    "• Автоматическое создание стикеров\n" +
                                    "• Оптимизация изображений\n" +
                                    "• Сохранение пропорций\n" +
                                    "• Поддержка PNG/WebP\n\n" +
                                    "💡 **Для создания стикера отправьте изображение!**")
                            .parseMode("Markdown")
                            .build();
                    sendWithLogging(infoMessage);
                    return;
                }
                
                if (text.contains("стикер") || text.contains("sticker") || text.contains("помощь") || text.contains("help")) {
                    SendMessage helpMessage = SendMessage.builder()
                            .chatId(msg.getChatId())
                            .text("🎯 **StickerBot** - создание стикеров из изображений\n\n" +
                                    "📸 **Как использовать:**\n" +
                                    "• Отправьте фото или изображение (JPG, PNG, GIF)\n" +
                                    "• Бот автоматически создаст стикер\n" +
                                    "• Готовый стикер будет отправлен в чат\n\n" +
                                    "⚠️ **Требования к изображению:**\n" +
                                    "• Формат: PNG, WebP\n" +
                                    "• Размер: до 512KB\n" +
                                    "• Разрешение: квадратное (512x512 пикселей)\n\n" +
                                    "🚀 **Начните прямо сейчас** - отправьте изображение!")
                            .parseMode("Markdown")
                            .build();
                    sendWithLogging(helpMessage);
                } else {
                    SendMessage infoMessage = SendMessage.builder()
                            .chatId(msg.getChatId())
                            .text("🎯 Отправьте изображение для создания стикера!\n\n" +
                                    "Поддерживаемые форматы: JPG, PNG, GIF\n" +
                                    "Для справки напишите: помощь")
                            .build();
                    sendWithLogging(infoMessage);
                }
            } else if (msg.hasSticker()) {
                // Обрабатываем стикеры
                LOGGER.info("🎭 Получен стикер | ChatId: {} | Emoji: {}", msg.getChatId(), msg.getSticker().getEmoji());
                SendMessage response = SendMessage.builder()
                        .chatId(msg.getChatId())
                        .text("🎭 Вы отправили стикер! Для создания нового стикера отправьте изображение (фото или документ).")
                        .build();
                sendWithLogging(response);
            } else if (msg.hasVideo() || msg.hasAudio()) {
                // Обрабатываем видео и аудио
                LOGGER.info("🎬 Получено видео/аудио | ChatId: {} | Тип: {}", msg.getChatId(), msg.hasVideo() ? "VIDEO" : "AUDIO");
                SendMessage response = SendMessage.builder()
                        .chatId(msg.getChatId())
                        .text("🎬 Для создания стикера отправьте изображение (фото или документ), а не видео/аудио.")
                        .build();
                sendWithLogging(response);
            } else {
                // Обрабатываем другие типы сообщений
                LOGGER.info("❓ Неизвестный тип сообщения | ChatId: {} | Тип: OTHER", msg.getChatId());
                SendMessage response = SendMessage.builder()
                        .chatId(msg.getChatId())
                        .text("📸 Для создания стикера отправьте изображение (фото или документ).\n\n" +
                                "Поддерживаемые форматы: JPG, PNG, GIF")
                        .build();
                sendWithLogging(response);
            }
        }
    }
} 