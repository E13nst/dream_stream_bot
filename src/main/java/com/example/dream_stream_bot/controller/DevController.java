package com.example.dream_stream_bot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Контроллер для разработки (только в dev профиле)
 * Предоставляет тестовые данные для Swagger UI
 */
@RestController
@RequestMapping("/dev")
@Profile("dev")
@Tag(name = "Разработка", description = "Тестовые эндпоинты для разработки (только в dev профиле)")
public class DevController {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(DevController.class);
    
    @Value("${telegram.test.init-data:}")
    private String testInitData;
    
    @Value("${telegram.test.bot-name:}")
    private String testBotName;
    
    /**
     * Получить тестовый initData для Swagger UI
     */
    @GetMapping("/test-init-data")
    @Operation(
        summary = "Получить тестовый initData",
        description = "Возвращает тестовый initData для использования в Swagger UI (только в dev профиле)"
    )
    @ApiResponse(responseCode = "200", description = "Тестовый initData получен",
        content = @Content(schema = @Schema(implementation = Map.class),
            examples = @ExampleObject(value = """
                {
                    "initData": "query_id=AAHdF6IQAAAAAN0XohDhrOrc&user=%7B%22id%22%3A123456789%7D&auth_date=1640995200&hash=test_hash_for_development_only",
                    "botName": "StickerGallery",
                    "message": "Используйте этот initData в Swagger UI для тестирования API"
                }
                """)))
    public ResponseEntity<Map<String, Object>> getTestInitData() {
        LOGGER.info("🔧 Запрос тестового initData для разработки");
        
        Map<String, Object> response = new HashMap<>();
        response.put("initData", testInitData);
        response.put("botName", testBotName);
        response.put("message", "Используйте этот initData в Swagger UI для тестирования API");
        response.put("warning", "ВНИМАНИЕ: Используйте только для разработки!");
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Диагностический эндпоинт для проверки заголовков
     */
    @GetMapping("/debug-headers")
    @Operation(
        summary = "Диагностика заголовков",
        description = "Возвращает все заголовки запроса для диагностики"
    )
    @ApiResponse(responseCode = "200", description = "Заголовки получены")
    public ResponseEntity<Map<String, Object>> debugHeaders(jakarta.servlet.http.HttpServletRequest request) {
        LOGGER.info("🔧 Запрос диагностики заголовков");
        
        Map<String, Object> response = new HashMap<>();
        Map<String, String> headers = new HashMap<>();
        
        // Получаем все заголовки
        java.util.Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            String headerValue = request.getHeader(headerName);
            headers.put(headerName, headerValue);
        }
        
        response.put("headers", headers);
        response.put("requestURI", request.getRequestURI());
        response.put("method", request.getMethod());
        response.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Получить информацию о dev профиле
     */
    @GetMapping("/info")
    @Operation(
        summary = "Информация о dev профиле",
        description = "Возвращает информацию о настройках dev профиля"
    )
    @ApiResponse(responseCode = "200", description = "Информация получена")
    public ResponseEntity<Map<String, Object>> getDevInfo() {
        LOGGER.info("🔧 Запрос информации о dev профиле");
        
        Map<String, Object> response = new HashMap<>();
        response.put("profile", "dev");
        response.put("testInitDataAvailable", testInitData != null && !testInitData.isEmpty());
        response.put("testBotName", testBotName);
        response.put("swaggerUrl", "/swagger-ui.html");
        response.put("openApiUrl", "/v3/api-docs");
        
        return ResponseEntity.ok(response);
    }
}
