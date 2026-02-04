# Implementation Plan: PhotoGrabber MVP

## Overview
PhotoGrabber is a Telegram bot that automatically downloads photos from group chats and stores them in MinIO (S3-compatible storage).

## Technology Stack
- **Kotlin 2.2.21** - programming language
- **Ktor 3.3.3** - web framework
- **tgbotapi 30.0.2** - Telegram Bot API client
- **MinIO 8.5.12** - S3-compatible storage client
- **Koin 4.1.2-Beta1** - dependency injection
- **Logback 1.4.14** - logging

## Architecture

```
Group Chat → Telegram Bot API → FileProcessor → MinIO → Storage
```

## Components

### 1. Configuration
- **TelegramConfig**: Bot token, list of group IDs to monitor
- **MinioConfig**: MinIO endpoint, access key, secret key, bucket name

### 2. Services
- **MinioService**: Handle MinIO connection, bucket management, file upload
- **TelegramBotService**: Bot initialization, event handling, group monitoring
- **FileProcessor**: Download files from Telegram, process metadata, upload to MinIO

### 3. Application
- **Application.kt**: Main entry point, configure Koin, initialize services

## Implementation Steps

### Step 1: Dependencies and Configuration
- Add MinIO client dependency (`io.minio:minio:8.5.12`)
- Add Kotlinx Serialization
- Configure environment variables in `application.yaml`:
  - `telegram.bot-token`
  - `telegram.groups-to-monitor` (list)
  - `minio.endpoint`
  - `minio.access-key`
  - `minio.secret-key`
  - `minio.bucket`

### Step 2: Configuration Classes
Create configuration dataclasses for Telegram and MinIO settings

### Step 3: MinIO Service
- Initialize MinIO client
- Check/create bucket on startup
- Implement `uploadFile(bytes: ByteArray, fileName: String, metadata: Map<String, String>)`
- Generate unique file names using format: `{group_id}/{date}/{timestamp}_{file_id}.jpg`

### Step 4: Telegram Bot Service
- Initialize bot with API token
- Subscribe to message updates from monitored groups
- Filter messages containing photos
- Extract photo metadata (file_id, group_id, date, user_id)

### Step 5: File Processor
- Download highest resolution photo from Telegram
- Extract EXIF data (optional, not for MVP)
- Build file path and metadata
- Call MinIO Service to upload
- Log success/error

### Step 6: Application Setup
- Configure Koin modules with all services
- Initialize Telegram Bot Service
- Add health check endpoint (`GET /health`)
- Graceful shutdown handling

### Step 7: Error Handling
- Network errors retry logic
- File download failures
- MinIO upload failures
- Rate limiting handling

### Step 8: Logging
- Configure Logback for structured logging
- Log each photo download/upload
- Log errors with full context

### Step 9: Testing
- Unit tests for MinIO Service
- Integration tests with test MinIO instance
- Manual testing with Telegram bot
- Load testing with multiple photos

## File Structure

```
src/main/kotlin/
├── Application.kt
├── io.r03el.photograbber.config/
│   ├── TelegramConfig.kt
│   └── MinioConfig.kt
├── service/
│   ├── MinioService.kt
│   ├── TelegramBotService.kt
│   └── FileProcessor.kt
└── Frameworks.kt
```

## Configuration Example

```yaml
telegram:
  bot-token: ${BOT_TOKEN}
  groups-to-monitor:
    - -100123456789
    - -100987654321

minio:
  endpoint: http://localhost:9000
  access-key: ${MINIO_ACCESS_KEY}
  secret-key: ${MINIO_SECRET_KEY}
  bucket: photograbber
```

## Future Enhancements (Post-MVP)
- User commands (`/stats`, `/groups`, `/status`)
- Date range filtering
- Rate limiting
- Web UI for monitoring
- Support for videos and documents
- Duplicate detection
- Album support
- Download from private chats
- Download from channels
