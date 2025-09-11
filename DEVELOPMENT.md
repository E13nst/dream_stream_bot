# 🚀 Разработка Telegram Bot Dream Stream

## 📋 Быстрый старт

### Основные команды

```bash
# Показать все доступные команды
make help

# Запустить приложение локально
make start

# Остановить приложение
make stop

# Перезапустить приложение
make restart

# Показать статус приложения
make status

# Показать логи
make logs

# Следить за логами в реальном времени
make logs-follow
```

## 🔧 Команды для разработки

### Управление приложением

```bash
# Запустить и сразу следить за логами
make dev-start

# Перезапустить и следить за логами
make dev-restart

# Проверить статус
make status
```

### Тестирование

```bash
# Запустить тесты
make test

# Тестировать API локально
make test-api

# Собрать приложение
make build
```

### Отладка

```bash
# Показать только ошибки из логов
make debug-logs

# Проверить, что порт свободен
make debug-port

# Очистить проект
make clean
```

## 📝 Git команды

```bash
# Сделать коммит с интерактивным вводом сообщения
make commit

# Развернуть на продакшен (push в main)
make deploy
```

## 🌐 Доступ к приложению

После запуска приложение будет доступно по адресам:

- **Основное приложение:** http://localhost:8080
- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **Mini App:** http://localhost:8080/mini-app/index.html
- **API статус:** http://localhost:8080/auth/status

## 📁 Структура проекта

```
src/
├── main/
│   ├── java/com/example/dream_stream_bot/
│   │   ├── bot/                    # Telegram бот
│   │   ├── config/                 # Конфигурация Spring
│   │   ├── controller/             # REST контроллеры
│   │   ├── model/                  # Модели данных
│   │   ├── security/               # Spring Security
│   │   ├── service/                # Бизнес-логика
│   │   └── util/                   # Утилиты
│   └── resources/
│       ├── static/mini-app/        # Telegram Mini App
│       └── application.yml         # Конфигурация
└── test/                           # Тесты
```

## 🔐 Переменные окружения

Приложение использует файл `.env.app` для локальной разработки:

```bash
# База данных
DB_HOST=localhost
DB_NAME=dream_stream
DB_USERNAME=username
DB_PASSWORD=password

# Telegram Bot Token
TELEGRAM_BOT_TOKEN=your_bot_token_here
```

## 🐛 Отладка

### Логи

```bash
# Показать последние логи
make logs

# Следить за логами в реальном времени
make logs-follow

# Показать только ошибки
make debug-logs
```

### Проблемы с портом

```bash
# Проверить, что порт свободен
make debug-port

# Если порт занят, остановить все процессы
make stop
```

### Тестирование API

```bash
# Тестировать авторизацию
make test-api

# Или вручную через curl
curl -X GET "http://localhost:8080/auth/status" \
  -H "accept: application/json" \
  -H "X-Telegram-Init-Data: your_init_data_here" \
  -H "X-Telegram-Bot-Name: StickerGallery"
```

## 🚀 Развертывание

### Локальная разработка

1. Убедитесь, что файл `.env.app` существует
2. Запустите приложение: `make start`
3. Проверьте статус: `make status`
4. Следите за логами: `make logs-follow`

### Продакшен

1. Сделайте изменения в коде
2. Закоммитьте: `make commit`
3. Разверните: `make deploy`
4. Дождитесь автоматического развертывания (2-3 минуты)

## 📚 Полезные ссылки

- [Telegram Bot API](https://core.telegram.org/bots/api)
- [Telegram Web Apps](https://core.telegram.org/bots/webapps)
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Spring Security](https://spring.io/projects/spring-security)

## 🤝 Поддержка

При возникновении проблем:

1. Проверьте логи: `make debug-logs`
2. Убедитесь, что порт свободен: `make debug-port`
3. Перезапустите приложение: `make restart`
4. Очистите проект: `make clean && make build`
