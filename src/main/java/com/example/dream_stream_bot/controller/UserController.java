package com.example.dream_stream_bot.controller;

import com.example.dream_stream_bot.dto.UserDto;
import com.example.dream_stream_bot.model.user.UserEntity;
import com.example.dream_stream_bot.service.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;
import java.util.Optional;

/**
 * Контроллер для работы с пользователями
 */
@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
@Tag(name = "Пользователи", description = "Управление пользователями системы")
@SecurityRequirement(name = "TelegramInitData")
public class UserController {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(UserController.class);
    
    private final UserService userService;
    
    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }
    
    /**
     * Получить всех пользователей
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Получить всех пользователей",
        description = "Возвращает список всех пользователей системы (только для ADMIN)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Список пользователей получен",
            content = @Content(schema = @Schema(implementation = UserDto.class),
                examples = @ExampleObject(value = """
                    [
                        {
                            "id": 1,
                            "telegramId": 123456789,
                            "username": "testuser",
                            "firstName": "Test",
                            "lastName": "User",
                            "role": "USER"
                        }
                    ]
                    """))),
        @ApiResponse(responseCode = "403", description = "Доступ запрещен"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<List<UserDto>> getAllUsers() {
        try {
            LOGGER.info("📋 Получение всех пользователей");
            List<UserDto> users = userService.findAllAsDto();
            LOGGER.info("✅ Найдено {} пользователей", users.size());
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при получении пользователей: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Получить пользователя по ID
     */
    @GetMapping("/{id}")
    @Operation(
        summary = "Получить пользователя по ID",
        description = "Возвращает информацию о пользователе по его ID"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Пользователь найден",
            content = @Content(schema = @Schema(implementation = UserDto.class),
                examples = @ExampleObject(value = """
                    {
                        "id": 1,
                        "telegramId": 123456789,
                        "username": "testuser",
                        "firstName": "Test",
                        "lastName": "User",
                        "role": "USER"
                    }
                    """))),
        @ApiResponse(responseCode = "404", description = "Пользователь не найден"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<UserDto> getUserById(
            @Parameter(description = "ID пользователя", required = true, example = "1")
            @PathVariable Long id) {
        try {
            LOGGER.info("🔍 Поиск пользователя по ID: {}", id);
            Optional<UserEntity> userOpt = userService.findById(id);
            
            if (userOpt.isPresent()) {
                UserDto userDto = UserDto.fromEntity(userOpt.get());
                LOGGER.info("✅ Пользователь найден: {}", userDto.getUsername());
                return ResponseEntity.ok(userDto);
            } else {
                LOGGER.warn("⚠️ Пользователь с ID {} не найден", id);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при поиске пользователя с ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Получить пользователя по telegram_id
     */
    @GetMapping("/telegram/{telegramId}")
    @Operation(
        summary = "Получить пользователя по Telegram ID",
        description = "Возвращает информацию о пользователе по его Telegram ID"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Пользователь найден"),
        @ApiResponse(responseCode = "404", description = "Пользователь не найден"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<UserDto> getUserByTelegramId(
            @Parameter(description = "Telegram ID пользователя", required = true, example = "123456789")
            @PathVariable Long telegramId) {
        try {
            LOGGER.info("🔍 Поиск пользователя по telegram_id: {}", telegramId);
            Optional<UserEntity> userOpt = userService.findByTelegramId(telegramId);
            
            if (userOpt.isPresent()) {
                UserDto userDto = UserDto.fromEntity(userOpt.get());
                LOGGER.info("✅ Пользователь найден: {}", userDto.getUsername());
                return ResponseEntity.ok(userDto);
            } else {
                LOGGER.warn("⚠️ Пользователь с telegram_id {} не найден", telegramId);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при поиске пользователя с telegram_id {}: {}", telegramId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Получить пользователя по username
     */
    @GetMapping("/username/{username}")
    @Operation(
        summary = "Получить пользователя по username",
        description = "Возвращает информацию о пользователе по его username"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Пользователь найден"),
        @ApiResponse(responseCode = "404", description = "Пользователь не найден"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<UserDto> getUserByUsername(
            @Parameter(description = "Username пользователя", required = true, example = "testuser")
            @PathVariable String username) {
        try {
            LOGGER.info("🔍 Поиск пользователя по username: {}", username);
            Optional<UserEntity> userOpt = userService.findByUsername(username);
            
            if (userOpt.isPresent()) {
                UserDto userDto = UserDto.fromEntity(userOpt.get());
                LOGGER.info("✅ Пользователь найден: {}", userDto.getUsername());
                return ResponseEntity.ok(userDto);
            } else {
                LOGGER.warn("⚠️ Пользователь с username {} не найден", username);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при поиске пользователя с username {}: {}", username, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Создать нового пользователя
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Создать нового пользователя",
        description = "Создает нового пользователя в системе. Доступно только администраторам. Все поля обязательны для заполнения."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Пользователь успешно создан",
            content = @Content(schema = @Schema(implementation = UserDto.class),
                examples = @ExampleObject(value = """
                    {
                        "id": 5,
                        "telegramId": 999999999,
                        "username": "newuser123",
                        "firstName": "New",
                        "lastName": "User",
                        "role": "USER",
                        "createdAt": "2025-09-15T14:30:00",
                        "updatedAt": "2025-09-15T14:30:00"
                    }
                    """))),
        @ApiResponse(responseCode = "400", description = "Некорректные данные - ошибки валидации",
            content = @Content(examples = @ExampleObject(value = """
                {
                    "validationErrors": {
                        "telegramId": "Telegram ID должен быть положительным числом",
                        "username": "Username может содержать только буквы, цифры и подчеркивания",
                        "role": "Роль должна быть USER или ADMIN"
                    },
                    "error": "Ошибка валидации",
                    "message": "Некорректные данные в запросе"
                }
                """))),
        @ApiResponse(responseCode = "401", description = "Не авторизован - требуется Telegram Web App авторизация"),
        @ApiResponse(responseCode = "403", description = "Доступ запрещен - требуется роль ADMIN"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<UserDto> createUser(
            @Parameter(description = "Данные для создания пользователя", required = true)
            @Valid @RequestBody UserDto userDto) {
        try {
            LOGGER.info("🆕 Создание нового пользователя: {}", userDto.getUsername());
            
            // Проверяем, что пользователь с таким telegramId не существует
            if (userService.existsByTelegramId(userDto.getTelegramId())) {
                LOGGER.warn("⚠️ Пользователь с telegram_id {} уже существует", userDto.getTelegramId());
                return ResponseEntity.badRequest().build();
            }
            
            UserEntity userEntity = userDto.toEntity();
            UserEntity savedUser = userService.save(userEntity);
            UserDto savedUserDto = UserDto.fromEntity(savedUser);
            
            LOGGER.info("✅ Пользователь создан: {} (ID: {})", savedUserDto.getUsername(), savedUserDto.getId());
            return ResponseEntity.status(201).body(savedUserDto);
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при создании пользователя: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Удалить пользователя
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Удалить пользователя",
        description = "Удаляет пользователя из системы (только для ADMIN)"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Пользователь удален"),
        @ApiResponse(responseCode = "403", description = "Доступ запрещен"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
    })
    public ResponseEntity<Void> deleteUser(
            @Parameter(description = "ID пользователя", required = true, example = "1")
            @PathVariable Long id) {
        try {
            LOGGER.info("🗑️ Удаление пользователя с ID: {}", id);
            userService.deleteById(id);
            LOGGER.info("✅ Пользователь с ID {} удален", id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            LOGGER.error("❌ Ошибка при удалении пользователя с ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
