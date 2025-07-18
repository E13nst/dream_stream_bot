# Деплой Telegram бота на Amvera

## Подготовка к деплою

### 1. Настройка переменных окружения

В Amvera нужно настроить следующие переменные окружения:

- `TELEGRAM_API_TOKEN` - токен вашего Telegram бота
- `TELEGRAM_BOT_NAME` - имя бота
- `OPENAI_API_KEY` - ключ API OpenAI
- `BOT_WEBHOOK_URL` - URL вашего приложения на Amvera (например: `https://your-app-name.amvera.io`)

### 2. Настройка Telegram Webhook

После деплоя приложения нужно установить веб-хук для Telegram бота. 
Это происходит автоматически при запуске приложения в продакшн режиме.

### 3. Деплой через Amvera CLI

```bash
# Установка Amvera CLI
npm install -g @amvera/cli

# Авторизация
amvera login

# Создание приложения (если еще не создано)
amvera app create your-app-name

# Деплой
amvera deploy
```

### 4. Деплой через Git

```bash
# Добавление remote для Amvera
git remote add amvera https://git.amvera.io/your-app-name.git

# Пуш в Amvera
git push amvera main
```

## Конфигурация

### amvera.yml

Файл `amvera.yml` содержит конфигурацию для деплоя:

- **Java 17** - версия Java
- **Maven** - система сборки
- **Порт 8080** - порт приложения
- **Health check** - проверка здоровья приложения
- **Автоскейлинг** - автоматическое масштабирование

### Профили Spring Boot

- **dev** - для локальной разработки (long polling)
- **prod** - для продакшена (webhook)

## Мониторинг

### Health Check

Приложение предоставляет endpoint для проверки здоровья:
- `GET /actuator/health` - статус приложения
- `GET /actuator/info` - информация о приложении
- `GET /actuator/metrics` - метрики

### Логи

Логи доступны в консоли Amvera:
```bash
amvera logs your-app-name
```

## Troubleshooting

### Проблемы с Webhook

1. Проверьте, что `BOT_WEBHOOK_URL` правильно настроен
2. Убедитесь, что приложение доступно по HTTPS
3. Проверьте логи на наличие ошибок установки webhook

### Проблемы с переменными окружения

1. Проверьте, что все переменные окружения настроены в Amvera
2. Убедитесь, что токены корректны
3. Проверьте права доступа к API

### Проблемы с памятью

Если приложение падает из-за нехватки памяти:
1. Увеличьте лимиты памяти в `amvera.yml`
2. Проверьте утечки памяти в коде
3. Настройте GC параметры JVM 

---

## 1. **Таблица ботов**

```sql
<code_block_to_apply_changes_from>
```
- `id` — внутренний ключ.
- `bot_uid` — уникальный идентификатор (можно использовать username или что-то своё).
- `telegram_bot_id` — Telegram Bot ID (числовой).
- `name` — человекочитаемое имя.

---

## 2. **Таблица истории чата (chat_memory)**

```sql
CREATE TABLE chat_memory (
    id SERIAL PRIMARY KEY,
    bot_id INT NOT NULL REFERENCES bot(id),
    user_id BIGINT NOT NULL,           -- id пользователя Telegram
    chat_id BIGINT,                    -- id чата (группы/канала), для приватных чатов — NULL
    message_index INT NOT NULL,
    role VARCHAR(16) NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    -- Составной уникальный ключ для истории:
    UNIQUE (bot_id, user_id, chat_id, message_index)
);
```

---

## 3. **Пояснения по полям**

- **bot_id** — внешний ключ на таблицу ботов. Позволяет хранить историю для разных ботов.
- **user_id** — id пользователя Telegram. Для групповых чатов — id отправителя, для приватных — id пользователя.
- **chat_id** — id группы/канала. Для приватных чатов — NULL (или можно дублировать user_id, если так удобнее).
- **message_index** — порядковый номер сообщения в рамках диалога (для конкретного сочетания bot_id, user_id, chat_id).
- **role, content, created_at** — как и раньше.
- **UNIQUE (bot_id, user_id, chat_id, message_index)** — обеспечивает уникальность сообщений в рамках одного диалога.

---

## 4. **Как формировать ключ диалога в коде**

- Для приватного чата:  
  - `bot_id` = id бота  
  - `user_id` = id пользователя  
  - `chat_id` = NULL  
- Для группового чата:  
  - `bot_id` = id бота  
  - `user_id` = id пользователя (отправителя)  
  - `chat_id` = id группы

---

## 5. **Пример запроса истории**

```sql
SELECT * FROM chat_memory
WHERE bot_id = :botId
  AND user_id = :userId
  AND (chat_id = :chatId OR (:chatId IS NULL AND chat_id IS NULL))
ORDER BY message_index ASC
LIMIT :lastN OFFSET GREATEST(0, (SELECT COUNT(*) FROM chat_memory WHERE ...) - :lastN)
```

---

## 6. **Преимущества такого подхода**

- Можно хранить историю для любого количества ботов.
- Гарантируется раздельный контекст для каждого пользователя и чата.
- Легко расширять (например, добавить поддержку других мессенджеров).

---

**Если хотите — могу подготовить DDL-скрипты, JPA-entity и пример репозитория под такую структуру!**  
Сообщите, если нужно реализовать это в коде или есть дополнительные пожелания по полям. 