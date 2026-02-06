# PhotoGrabber

Приложение для автоматического сбора фото и видео из Telegram-групп с загрузкой в MinIO хранилище и веб-галереей.

## Возможности

- **Telegram бот** — мониторинг групп и автоматическая загрузка медиафайлов
- **MinIO интеграция** — хранение файлов в S3-совместимом хранилище
- **SQLite очередь** — надежная обработка файлов с повторными попытками
- **Веб-галерея** — просмотр загруженных файлов через HTTP
- **Гибкая конфигурация** — через YAML и переменные окружения

## Требования

- Java 21
- Gradle 8.5+

## Быстрый старт

### 1. Настройка окружения

```bash
export BOT_TOKEN="your_telegram_bot_token"
export MINIO_ENDPOINT="http://localhost:9000"
export MINIO_ACCESS_KEY="minioadmin"
export MINIO_SECRET_KEY="minioadmin"
export MINIO_BUCKET="photos"
```

### 2. Настройка через YAML

Отредактируйте `src/main/resources/application.yaml`:

```yaml
telegram:
    enableFilter: false
    groupsToMonitor:
        - -4991977984  # ID групп для мониторинга

queue:
    sqlite:
        path: "./data/queue.db"

worker:
    enabled: true
    concurrency: 3
    pollIntervalMs: 1000
    maxRetries: 3
```

### 3. Сборка и запуск

```bash
# Сборка
./gradlew build

# Запуск
./gradlew run

# Или fat JAR
./gradlew buildFatJar
java -jar build/libs/photograbber-0.0.1-all.jar
```

## Команды Gradle

| Команда | Описание |
|---------|----------|
| `./gradlew build` | Сборка проекта |
| `./gradlew run` | Запуск приложения |
| `./gradlew test` | Запуск тестов |
| `./gradlew buildFatJar` | Сборка исполняемого JAR |
| `./gradlew clean` | Очистка сборки |

## Архитектура

```
src/main/kotlin/io/r03el/photograbber/
├── Application.kt          # Точка входа
├── AppModule.kt            # DI конфигурация (Koin)
├── config/                 # Конфигурация
├── db/                     # База данных и миграции
│   └── repository/         # Репозитории
├── model/                  # Модели данных
├── server/                 # HTTP сервер и контроллеры
└── service/                # Бизнес-логика
```

## Технологии

- **Kotlin** 1.9.20
- **Koin** 3.5.6 — Dependency Injection
- **SQLite** — локальная база данных
- **MinIO** — S3-совместимое хранилище
- **kotlin-telegram-bot** 6.3.0 — работа с Telegram API
- **kotlinx.serialization** — сериализация

## Лицензия

MIT
