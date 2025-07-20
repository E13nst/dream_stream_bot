# Telegram Dream Stream Bot

Telegram бот для анализа снов с использованием OpenAI GPT.

## 🚀 Быстрый старт

### Локальная разработка

1. **Клонируйте репозиторий:**
```bash
git clone <repository-url>
cd telegram-bot-dream-stream
```

2. **Создайте файл `.env.app` с вашими переменными:**
```bash
# Telegram Bot Configuration
TELEGRAM_API_TOKEN=your_telegram_bot_token_here
TELEGRAM_BOT_NAME=your_bot_name_here

# OpenAI Configuration
OPENAI_API_KEY=your_openai_api_key_here
```

3. **Запустите приложение:**
```bash
export $(cat .env.app | xargs) && ./gradlew bootRun --args='--spring.profiles.active=dev'
```

### Деплой на Amvera

1. **Создайте переменные окружения в панели Amvera:**

   Перейдите в раздел **Переменные окружения** вашего приложения и добавьте:

   | Переменная | Описание | Пример значения |
   |------------|----------|-----------------|
   | `TELEGRAM_API_TOKEN` | Токен вашего Telegram бота | `1234567890:ABCdefGHIjklMNOpqrsTUVwxyz` |
   | `TELEGRAM_BOT_NAME` | Имя вашего бота | `my_dream_bot` |
   | `OPENAI_API_KEY` | API ключ OpenAI | `sk-...` |
   | `BOT_WEBHOOK_URL` | URL для webhook (ваш домен) | `https://your-app.amvera.io/webhook` |

2. **Получите ваш домен:**
   - После создания приложения на Amvera, вам будет присвоен домен вида `your-app.amvera.io`
   - Используйте его для формирования `BOT_WEBHOOK_URL`

3. **Деплой:**
```bash
git add .
git commit -m "Deploy to Amvera"
git push origin main
```

## 🔧 Конфигурация

### Переменные окружения

| Переменная | Обязательная | Описание |
|------------|--------------|----------|
| `TELEGRAM_API_TOKEN` | ✅ | Токен бота от @BotFather |
| `TELEGRAM_BOT_NAME` | ✅ | Имя бота |
| `OPENAI_API_KEY` | ✅ | API ключ OpenAI |
| `BOT_WEBHOOK_URL` | ✅ | URL для webhook (только для продакшена) |
| `SERVER_PORT` | ❌ | Порт сервера (по умолчанию: 8080) |

### Профили

- **dev** - локальная разработка с long polling
- **prod** - продакшен с webhook

## 📝 Функциональность

- 💬 Обычные сообщения обрабатываются через OpenAI
- 🌙 Анализ снов с пошаговым процессом
- 🔘 Интерактивные кнопки
- 📊 История снов

## 🔒 Безопасность

⚠️ **ВАЖНО:** Никогда не коммитьте секретные данные в репозиторий!

- Файл `.env.app` добавлен в `.gitignore`
- Все секреты передаются через переменные окружения
- В продакшене используйте переменные окружения хостинга

## 🛠 Технологии

- Java 17
- Spring Boot 3.3.3
- Telegram Bot API
- OpenAI GPT API
- Gradle

## 📞 Поддержка

При возникновении проблем проверьте:
1. Правильность переменных окружения
2. Доступность API ключей
3. Логи приложения в панели Amvera

