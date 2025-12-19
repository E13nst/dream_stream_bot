#!/bin/bash

# Скрипт для тестирования API управления ботами
# Использование: ./test_bot_api.sh

BASE_URL="http://localhost:8080/api/bots"

echo "🧪 Тестирование API управления ботами"
echo "========================================"
echo ""

# Проверка доступности API
echo "1. Проверка доступности API..."
if curl -s -f "$BASE_URL" > /dev/null 2>&1; then
    echo "✅ API доступно"
else
    echo "❌ API недоступно. Убедитесь, что приложение запущено на порту 8080"
    exit 1
fi
echo ""

# 1. Получить всех ботов
echo "2. GET /api/bots - Получить всех ботов"
response=$(curl -s -w "\n%{http_code}" "$BASE_URL")
http_code=$(echo "$response" | tail -n1)
body=$(echo "$response" | sed '$d')
echo "HTTP Code: $http_code"
echo "Response:"
echo "$body" | jq '.' 2>/dev/null || echo "$body"
echo ""

# 2. Создать нового бота
echo "3. POST /api/bots - Создать нового бота"
create_request='{
  "name": "Test Bot",
  "username": "test_bot_'$(date +%s)'",
  "token": "123456:ABC-DEF123456",
  "type": "ASSISTANT",
  "description": "Тестовый бот для проверки API",
  "isActive": true,
  "memWindow": 100
}'
response=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -d "$create_request")
http_code=$(echo "$response" | tail -n1)
body=$(echo "$response" | sed '$d')
echo "HTTP Code: $http_code"
echo "Response:"
echo "$body" | jq '.' 2>/dev/null || echo "$body"

# Извлекаем ID созданного бота
BOT_ID=$(echo "$body" | jq -r '.id' 2>/dev/null)
if [ "$BOT_ID" != "null" ] && [ -n "$BOT_ID" ]; then
    echo "✅ Бот создан с ID: $BOT_ID"
    echo ""
    
    # 3. Получить бота по ID
    echo "4. GET /api/bots/$BOT_ID - Получить бота по ID"
    response=$(curl -s -w "\n%{http_code}" "$BASE_URL/$BOT_ID")
    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | sed '$d')
    echo "HTTP Code: $http_code"
    echo "Response:"
    echo "$body" | jq '.' 2>/dev/null || echo "$body"
    echo ""
    
    # 4. Обновить бота
    echo "5. PUT /api/bots/$BOT_ID - Обновить бота"
    update_request='{
      "name": "Updated Test Bot",
      "description": "Обновленное описание",
      "isActive": false
    }'
    response=$(curl -s -w "\n%{http_code}" -X PUT "$BASE_URL/$BOT_ID" \
      -H "Content-Type: application/json" \
      -d "$update_request")
    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | sed '$d')
    echo "HTTP Code: $http_code"
    echo "Response:"
    echo "$body" | jq '.' 2>/dev/null || echo "$body"
    echo ""
    
    # 5. Обновить miniapp
    echo "6. PATCH /api/bots/$BOT_ID/miniapp - Обновить miniapp"
    response=$(curl -s -w "\n%{http_code}" -X PATCH "$BASE_URL/$BOT_ID/miniapp" \
      -H "Content-Type: application/json" \
      -d '"https://example.com/miniapp"')
    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | sed '$d')
    echo "HTTP Code: $http_code"
    echo "Response:"
    echo "$body" | jq '.' 2>/dev/null || echo "$body"
    echo ""
    
    # 6. Обновить prompt бота
    echo "7. PATCH /api/bots/$BOT_ID/prompt - Обновить prompt бота"
    new_prompt="You are a helpful assistant that helps users interpret their dreams. Be concise and friendly."
    response=$(curl -s -w "\n%{http_code}" -X PATCH "$BASE_URL/$BOT_ID/prompt" \
      -H "Content-Type: application/json" \
      -d "\"$new_prompt\"")
    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | sed '$d')
    echo "HTTP Code: $http_code"
    echo "Response:"
    echo "$body" | jq '.' 2>/dev/null || echo "$body"
    echo ""
    
    # 7. Получить активных ботов
    echo "8. GET /api/bots/active - Получить активных ботов"
    response=$(curl -s -w "\n%{http_code}" "$BASE_URL/active")
    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | sed '$d')
    echo "HTTP Code: $http_code"
    echo "Response:"
    echo "$body" | jq '.' 2>/dev/null || echo "$body"
    echo ""
    
    # 8. Получить ботов по типу
    echo "9. GET /api/bots/type/ASSISTANT - Получить ботов по типу"
    response=$(curl -s -w "\n%{http_code}" "$BASE_URL/type/ASSISTANT")
    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | sed '$d')
    echo "HTTP Code: $http_code"
    echo "Response:"
    echo "$body" | jq '.' 2>/dev/null || echo "$body"
    echo ""
    
    # 9. Проверка, что token скрыт в ответах
    echo "10. Проверка, что token скрыт в ответах API"
    response=$(curl -s "$BASE_URL/$BOT_ID")
    has_token=$(echo "$response" | jq -r 'has("token")' 2>/dev/null)
    token_value=$(echo "$response" | jq -r '.token' 2>/dev/null)
    if [ "$has_token" = "true" ] && [ "$token_value" = "null" ]; then
        echo "✅ Token правильно скрыт (null)"
    elif [ "$has_token" = "false" ]; then
        echo "✅ Token отсутствует в ответе"
    else
        echo "⚠️ Token присутствует в ответе: $token_value"
    fi
    echo ""
    
    # 10. Тест валидации - попытка создать бота с невалидными данными
    echo "11. POST /api/bots - Тест валидации (невалидные данные)"
    invalid_request='{
      "name": "",
      "username": "invalid username with spaces",
      "token": "short"
    }'
    response=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL" \
      -H "Content-Type: application/json" \
      -d "$invalid_request")
    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | sed '$d')
    echo "HTTP Code: $http_code (ожидается 400)"
    echo "Response:"
    echo "$body" | jq '.' 2>/dev/null || echo "$body"
    echo ""
    
    # 11. Удалить бота
    echo "12. DELETE /api/bots/$BOT_ID - Удалить бота"
    response=$(curl -s -w "\n%{http_code}" -X DELETE "$BASE_URL/$BOT_ID")
    http_code=$(echo "$response" | tail -n1)
    echo "HTTP Code: $http_code (ожидается 204)"
    if [ "$http_code" == "204" ]; then
        echo "✅ Бот успешно удален"
    else
        echo "❌ Ошибка при удалении бота"
    fi
    echo ""
    
    # 12. Попытка получить удаленного бота
    echo "13. GET /api/bots/$BOT_ID - Попытка получить удаленного бота"
    response=$(curl -s -w "\n%{http_code}" "$BASE_URL/$BOT_ID")
    http_code=$(echo "$response" | tail -n1)
    echo "HTTP Code: $http_code (ожидается 404)"
    if [ "$http_code" == "404" ]; then
        echo "✅ Бот не найден (как и ожидалось)"
    fi
    echo ""
else
    echo "❌ Не удалось создать бота для дальнейшего тестирования"
fi

echo "========================================"
echo "✅ Тестирование завершено!"
echo ""
echo "💡 Для просмотра Swagger документации откройте:"
echo "   http://localhost:8080/swagger-ui.html"

