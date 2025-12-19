package com.example.dream_stream_bot.controller;

import com.example.dream_stream_bot.dto.BotEntityDto;
import com.example.dream_stream_bot.dto.CreateBotRequest;
import com.example.dream_stream_bot.dto.UpdateBotRequest;
import com.example.dream_stream_bot.model.telegram.BotEntity;
import com.example.dream_stream_bot.model.telegram.BotType;
import com.example.dream_stream_bot.service.telegram.BotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Контроллер для управления ботами Telegram
 */
@RestController
@RequestMapping("/api/bots")
@CrossOrigin(origins = "*")
@Tag(name = "Боты", description = "API для управления ботами Telegram (создание, чтение, обновление, удаление)")
public class BotController {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(BotController.class);
    private final BotService botService;
    
    @Autowired
    public BotController(BotService botService) {
        this.botService = botService;
    }
    
    /**
     * Получить всех ботов
     */
    @GetMapping
    @Operation(
        summary = "Получить всех ботов",
        description = "Возвращает список всех ботов в системе"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Список ботов получен успешно",
            content = @Content(schema = @Schema(implementation = BotEntityDto.class),
                examples = @ExampleObject(value = """
                    [
                        {
                            "id": 1,
                            "name": "Dream Stream Bot",
                            "username": "dreamstream_bot",
                            "type": "assistant",
                            "isActive": true,
                            "createdAt": "2025-01-15T10:30:00",
                            "updatedAt": "2025-01-15T10:30:00"
                        }
                    ]
                    """))),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<List<BotEntityDto>> getAllBots() {
        try {
            LOGGER.info("📋 Получение всех ботов");
            List<BotEntity> bots = botService.findAll();
            List<BotEntityDto> dtos = bots.stream()
                    .map(BotEntityDto::fromEntity)
                    .collect(Collectors.toList());
            
            LOGGER.info("✅ Найдено {} ботов", dtos.size());
            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при получении всех ботов", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Получить бота по ID
     */
    @GetMapping("/{id}")
    @Operation(
        summary = "Получить бота по ID",
        description = "Возвращает информацию о боте по его уникальному идентификатору"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Бот найден",
            content = @Content(schema = @Schema(implementation = BotEntityDto.class),
                examples = @ExampleObject(value = """
                    {
                        "id": 1,
                        "name": "Dream Stream Bot",
                        "username": "dreamstream_bot",
                        "type": "assistant",
                        "prompt": "You are a helpful assistant",
                        "isActive": true,
                        "createdAt": "2025-01-15T10:30:00",
                        "updatedAt": "2025-01-15T10:30:00"
                    }
                    """))),
        @ApiResponse(responseCode = "404", description = "Бот не найден"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<BotEntityDto> getBotById(
            @Parameter(description = "ID бота", required = true, example = "1")
            @PathVariable Long id) {
        try {
            LOGGER.info("🔍 Поиск бота по ID: {}", id);
            BotEntity bot = botService.findById(id);
            
            if (bot == null) {
                LOGGER.warn("⚠️ Бот с ID {} не найден", id);
                return ResponseEntity.notFound().build();
            }
            
            BotEntityDto dto = BotEntityDto.fromEntity(bot);
            LOGGER.info("✅ Бот найден: {}", dto.getName());
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при поиске бота с ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Получить ботов по типу
     */
    @GetMapping("/type/{type}")
    @Operation(
        summary = "Получить ботов по типу",
        description = "Возвращает список ботов указанного типа. Поддерживаемые типы: COPYCAT, ASSISTANT"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Список ботов получен",
            content = @Content(schema = @Schema(implementation = BotEntityDto.class))),
        @ApiResponse(responseCode = "400", description = "Некорректный тип бота"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<List<BotEntityDto>> getBotsByType(
            @Parameter(description = "Тип бота (COPYCAT, ASSISTANT)", required = true, example = "ASSISTANT")
            @PathVariable String type) {
        try {
            LOGGER.info("🔍 Поиск ботов по типу: {}", type);
            
            // Валидация типа
            BotType botType;
            try {
                botType = BotType.fromString(type);
            } catch (IllegalArgumentException e) {
                LOGGER.warn("⚠️ Некорректный тип бота: {}", type);
                return ResponseEntity.badRequest().build();
            }
            
            List<BotEntity> bots = botService.findByType(botType.getValue());
            List<BotEntityDto> dtos = bots.stream()
                    .map(BotEntityDto::fromEntity)
                    .collect(Collectors.toList());
            
            LOGGER.info("✅ Найдено {} ботов типа {}", dtos.size(), type);
            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при поиске ботов типа: {}", type, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Получить активных ботов
     */
    @GetMapping("/active")
    @Operation(
        summary = "Получить активных ботов",
        description = "Возвращает список всех активных ботов (isActive = true)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Список активных ботов получен",
            content = @Content(schema = @Schema(implementation = BotEntityDto.class))),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<List<BotEntityDto>> getActiveBots() {
        try {
            LOGGER.info("🔍 Получение активных ботов");
            List<BotEntity> bots = botService.findActiveBots();
            List<BotEntityDto> dtos = bots.stream()
                    .map(BotEntityDto::fromEntity)
                    .collect(Collectors.toList());
            
            LOGGER.info("✅ Найдено {} активных ботов", dtos.size());
            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при получении активных ботов", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Получить бота по имени
     */
    @GetMapping("/name/{name}")
    @Operation(
        summary = "Получить бота по имени",
        description = "Возвращает информацию о боте по его отображаемому имени"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Бот найден",
            content = @Content(schema = @Schema(implementation = BotEntityDto.class))),
        @ApiResponse(responseCode = "404", description = "Бот не найден"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<BotEntityDto> getBotByName(
            @Parameter(description = "Имя бота", required = true, example = "Dream Stream Bot")
            @PathVariable String name) {
        try {
            LOGGER.info("🔍 Поиск бота по имени: {}", name);
            
            var botOpt = botService.findByName(name);
            if (botOpt.isEmpty()) {
                LOGGER.warn("⚠️ Бот с именем '{}' не найден", name);
                return ResponseEntity.notFound().build();
            }
            
            BotEntityDto dto = BotEntityDto.fromEntity(botOpt.get());
            LOGGER.info("✅ Бот найден: {}", dto.getName());
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при поиске бота с именем: {}", name, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Создать нового бота
     */
    @PostMapping
    @Operation(
        summary = "Создать нового бота",
        description = "Создает нового бота в системе. Обязательные поля: name, username, token, type"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Бот успешно создан",
            content = @Content(schema = @Schema(implementation = BotEntityDto.class),
                examples = @ExampleObject(value = """
                    {
                        "id": 1,
                        "name": "Dream Stream Bot",
                        "username": "dreamstream_bot",
                        "type": "assistant",
                        "isActive": true,
                        "createdAt": "2025-01-15T10:30:00",
                        "updatedAt": "2025-01-15T10:30:00"
                    }
                    """))),
        @ApiResponse(responseCode = "400", description = "Некорректные данные - ошибки валидации",
            content = @Content(examples = @ExampleObject(value = """
                {
                    "validationErrors": {
                        "name": "Имя бота обязательно для заполнения",
                        "username": "Username бота обязателен для заполнения",
                        "token": "Токен бота обязателен для заполнения",
                        "type": "Тип бота обязателен для заполнения"
                    },
                    "error": "Ошибка валидации",
                    "message": "Некорректные данные в запросе"
                }
                """))),
        @ApiResponse(responseCode = "409", description = "Бот с таким username уже существует"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<BotEntityDto> createBot(
            @Parameter(description = "Данные для создания бота", required = true)
            @Valid @RequestBody CreateBotRequest request) {
        try {
            LOGGER.info("➕ Создание нового бота: {}", request.getName());
            
            // Проверка уникальности username
            if (botService.existsByUsername(request.getUsername())) {
                LOGGER.warn("⚠️ Бот с username '{}' уже существует", request.getUsername());
                return ResponseEntity.status(HttpStatus.CONFLICT).build();
            }
            
            // Создание нового бота
            BotEntity newBot = new BotEntity();
            newBot.setName(request.getName());
            newBot.setUsername(request.getUsername());
            newBot.setToken(request.getToken());
            newBot.setType(request.getType().getValue());
            newBot.setPrompt(request.getPrompt());
            newBot.setWebhookUrl(request.getWebhookUrl());
            newBot.setDescription(request.getDescription());
            newBot.setTriggers(request.getTriggers());
            newBot.setMemWindow(request.getMemWindow() != null ? request.getMemWindow() : 100);
            newBot.setMiniapp(request.getMiniapp());
            newBot.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);
            
            BotEntity savedBot = botService.save(newBot);
            BotEntityDto createdDto = BotEntityDto.fromEntity(savedBot);
            
            LOGGER.info("✅ Бот создан с ID: {}", createdDto.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(createdDto);
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при создании бота", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Обновить существующего бота
     */
    @PutMapping("/{id}")
    @Operation(
        summary = "Обновить бота",
        description = "Обновляет информацию о боте. Все поля опциональные - обновляются только переданные поля"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Бот успешно обновлен",
            content = @Content(schema = @Schema(implementation = BotEntityDto.class))),
        @ApiResponse(responseCode = "400", description = "Некорректные данные - ошибки валидации"),
        @ApiResponse(responseCode = "404", description = "Бот не найден"),
        @ApiResponse(responseCode = "409", description = "Бот с таким username уже существует"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<BotEntityDto> updateBot(
            @Parameter(description = "ID бота", required = true, example = "")
            @PathVariable Long id,
            @Parameter(description = "Данные для обновления бота", required = true)
            @Valid @RequestBody UpdateBotRequest request) {
        try {
            LOGGER.info("✏️ Обновление бота с ID: {}", id);
            
            BotEntity existingBot = botService.findById(id);
            if (existingBot == null) {
                LOGGER.warn("⚠️ Бот с ID {} не найден для обновления", id);
                return ResponseEntity.notFound().build();
            }
            
            // Проверка уникальности username, если он изменяется
            if (request.getUsername() != null && !request.getUsername().equals(existingBot.getUsername())) {
                if (botService.existsByUsername(request.getUsername())) {
                    LOGGER.warn("⚠️ Бот с username '{}' уже существует", request.getUsername());
                    return ResponseEntity.status(HttpStatus.CONFLICT).build();
                }
            }
            
            // Обновляем поля
            if (request.getName() != null) {
                existingBot.setName(request.getName());
            }
            if (request.getUsername() != null) {
                existingBot.setUsername(request.getUsername());
            }
            if (request.getToken() != null) {
                existingBot.setToken(request.getToken());
            }
            if (request.getType() != null) {
                existingBot.setType(request.getType().getValue());
            }
            if (request.getPrompt() != null) {
                existingBot.setPrompt(request.getPrompt());
            }
            if (request.getWebhookUrl() != null) {
                existingBot.setWebhookUrl(request.getWebhookUrl());
            }
            if (request.getDescription() != null) {
                existingBot.setDescription(request.getDescription());
            }
            if (request.getTriggers() != null) {
                existingBot.setTriggers(request.getTriggers());
            }
            if (request.getMemWindow() != null) {
                existingBot.setMemWindow(request.getMemWindow());
            }
            if (request.getMiniapp() != null) {
                existingBot.setMiniapp(request.getMiniapp());
            }
            if (request.getIsActive() != null) {
                existingBot.setIsActive(request.getIsActive());
            }
            
            BotEntity updatedBot = botService.save(existingBot);
            BotEntityDto updatedDto = BotEntityDto.fromEntity(updatedBot);
            
            LOGGER.info("✅ Бот обновлен: {}", updatedDto.getName());
            return ResponseEntity.ok(updatedDto);
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при обновлении бота с ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Обновить только miniapp для бота по ID
     */
    @PatchMapping("/{id}/miniapp")
    @Operation(
        summary = "Обновить miniapp URL бота",
        description = "Обновляет только URL миниприложения для указанного бота"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Miniapp URL обновлен",
            content = @Content(schema = @Schema(implementation = BotEntityDto.class))),
        @ApiResponse(responseCode = "404", description = "Бот не найден"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<BotEntityDto> updateBotMiniapp(
            @Parameter(description = "ID бота", required = true, example = "1")
            @PathVariable Long id,
            @Parameter(description = "URL миниприложения", required = true, example = "https://example.com/miniapp")
            @RequestBody String miniappUrl) {
        try {
            LOGGER.info("🔧 Обновление miniapp для бота с ID {}: {}", id, miniappUrl);
            
            BotEntity existingBot = botService.findById(id);
            if (existingBot == null) {
                LOGGER.warn("⚠️ Бот с ID {} не найден", id);
                return ResponseEntity.notFound().build();
            }
            
            existingBot.setMiniapp(miniappUrl);
            BotEntity updatedBot = botService.save(existingBot);
            BotEntityDto updatedDto = BotEntityDto.fromEntity(updatedBot);
            
            LOGGER.info("✅ Miniapp обновлен для бота с ID {}: {}", id, miniappUrl);
            return ResponseEntity.ok(updatedDto);
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при обновлении miniapp для бота с ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Обновить только prompt для бота по ID
     */
    @PatchMapping("/{id}/prompt")
    @Operation(
        summary = "Обновить prompt бота",
        description = "Обновляет только промпт (системное сообщение) для указанного бота. Промпт используется для настройки поведения AI бота."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Prompt обновлен",
            content = @Content(schema = @Schema(implementation = BotEntityDto.class),
                examples = @ExampleObject(value = """
                    {
                        "id": 1,
                        "name": "Dream Stream Bot",
                        "username": "dreamstream_bot",
                        "prompt": "You are a helpful assistant that helps users interpret their dreams.",
                        "type": "assistant",
                        "isActive": true
                    }
                    """))),
        @ApiResponse(responseCode = "404", description = "Бот не найден"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<BotEntityDto> updateBotPrompt(
            @Parameter(description = "ID бота", required = true, example = "")
            @PathVariable Long id,
            @Parameter(description = "Новый промпт для бота", required = true, 
                example = "You are a helpful assistant that helps users interpret their dreams. Be concise and friendly.")
            @RequestBody String prompt) {
        try {
            LOGGER.info("🔧 Обновление prompt для бота с ID {}", id);
            
            BotEntity existingBot = botService.findById(id);
            if (existingBot == null) {
                LOGGER.warn("⚠️ Бот с ID {} не найден", id);
                return ResponseEntity.notFound().build();
            }
            
            existingBot.setPrompt(prompt);
            BotEntity updatedBot = botService.save(existingBot);
            BotEntityDto updatedDto = BotEntityDto.fromEntity(updatedBot);
            
            LOGGER.info("✅ Prompt обновлен для бота с ID {}", id);
            return ResponseEntity.ok(updatedDto);
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при обновлении prompt для бота с ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Удалить бота
     */
    @DeleteMapping("/{id}")
    @Operation(
        summary = "Удалить бота",
        description = "Удаляет бота из системы по его ID"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Бот успешно удален"),
        @ApiResponse(responseCode = "404", description = "Бот не найден"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<Void> deleteBot(
            @Parameter(description = "ID бота", required = true, example = "")
            @PathVariable Long id) {
        try {
            LOGGER.info("🗑️ Удаление бота с ID: {}", id);
            
            if (!botService.existsById(id)) {
                LOGGER.warn("⚠️ Бот с ID {} не найден для удаления", id);
                return ResponseEntity.notFound().build();
            }
            
            botService.deleteById(id);
            LOGGER.info("✅ Бот с ID {} удален", id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при удалении бота с ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
