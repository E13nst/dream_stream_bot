# Dream Interpretation Telegram Bot

This project is a Telegram bot designed to help with dream interpretation using methods from Jungian analysis. The bot provides users with an interactive experience to explore and understand the symbols and themes in their dreams.

You can find the bot on Telegram at https://t.me/einst_gpt_bot

## 🚀 Quick Start

### Prerequisites
- Java 17+
- Maven 3.6+
- Telegram Bot Token
- OpenAI API Key

### Setup

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd telegram-bot-dream-stream
   ```

2. **Configure environment variables**
   ```bash
   cp .env.example .env.app
   ```
   
   Edit `.env.app` and add your actual tokens:
   ```bash
   TELEGRAM_API_TOKEN=your_actual_telegram_token
   TELEGRAM_BOT_NAME=your_bot_name
   OPENAI_API_KEY=your_actual_openai_key
   ```

3. **Build and run**
   ```bash
   mvn clean package -DskipTests
   export $(cat .env.app | xargs) && java -jar target/telegram-bot-dream-stream-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
   ```

## 🔒 Security

- **Never commit secrets**: The `.env.app` file is ignored by git
- **Use environment variables**: All sensitive data should be passed via environment variables
- **Test configuration**: Test files use placeholder values, not real tokens

## 📁 Project Structure

- `src/main/resources/application.yaml` - Main configuration
- `src/main/resources/application-prod.yaml` - Production configuration
- `amvera.yml` - Deployment configuration for cloud.amvera.ru
- `docker-compose.yml` - Docker configuration

## 🐳 Docker

```bash
docker-compose up -d
```

## 📝 Features

- Interactive dream analysis using Jungian psychology
- AI-powered conversation with personality (Dalek the cat)
- Multi-step dream interpretation process
- Telegram webhook and long polling support

