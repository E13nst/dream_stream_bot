# Имя вашего JAR файла и Docker образа
APP_NAME = telegram-bot-dream-stream
VERSION = 0.0.1-SNAPSHOT
JAR_FILE = target/$(APP_NAME)-$(VERSION).jar
DOCKER_IMAGE = telegram-bot-dream-stream

# Задача по умолчанию
all: build

# Сборка JAR файла с использованием Maven
build:
	mvn clean package

# Создание Docker образа
docker-build: build
	docker build -t $(DOCKER_IMAGE):latest .

# Запуск Docker контейнера
docker-run: docker-build
	docker-compose up --build -d

# Остановка и удаление Docker контейнера
docker-down:
	docker-compose down

# Очистка файлов сборки и Docker образов
clean:
	mvn clean
	docker rmi $(DOCKER_IMAGE):latest

.PHONY: all build docker-build docker-run docker-down clean

# Запуск автотестов с использованием Maven
test: build
	mvn test