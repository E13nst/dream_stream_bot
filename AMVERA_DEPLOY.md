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