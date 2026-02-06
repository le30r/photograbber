# AGENTS.md - Coding Guidelines for photograbber

## Project Overview

A Kotlin/JVM application that grabs photos and videos from Telegram groups and uploads them to MinIO storage. Uses SQLite for queue management and provides a web gallery.

## Important note!

Учитывай что у меня по умолчанию 25 джава, которая не совместима с текущим проектом. Для запуска компиляции и проверок используй 21

## Build Commands

```bash
# Build the project
./gradlew build

# Run tests (all)
./gradlew test

# Run a single test class
./gradlew test --tests "ClassName"

# Run a single test method
./gradlew test --tests "ClassName.methodName"

# Run the application
./gradlew run

# Build fat JAR
./gradlew buildFatJar

# Clean build
./gradlew clean
```

## Technology Stack

- **Language**: Kotlin 1.9.20
- **JVM**: Java 21
- **Build Tool**: Gradle 8.5 with Kotlin DSL
- **DI Framework**: Koin 3.5.6
- **Database**: SQLite with JDBC
- **Storage**: MinIO (S3-compatible)
- **Telegram**: kotlin-telegram-bot 6.3.0
- **Serialization**: kotlinx.serialization

## Code Style Guidelines

### Formatting

- **Indentation**: 4 spaces (no tabs)
- **Line length**: 120 characters max
- **Trailing commas**: Required in multi-line parameter lists
- **Imports**: Use wildcard imports for packages with 3+ imports from same package

### Naming Conventions

- **Classes**: PascalCase (e.g., `QueueService`, `MediaFileMetadata`)
- **Functions/Variables**: camelCase (e.g., `processItem`, `fileId`)
- **Constants**: UPPER_SNAKE_CASE for top-level constants
- **Packages**: lowercase with dots (e.g., `io.r03el.photograbber.service`)
- **Private fields**: No underscore prefix, use `private val`

### Imports

Order: Kotlin stdlib → Third-party → Project internal

```kotlin
// Kotlin stdlib
import kotlinx.coroutines.*
import java.sql.Connection

// Third-party
import org.koin.core.context.GlobalContext.startKoin
import org.slf4j.LoggerFactory
import io.minio.*

// Project internal
import io.r03el.photograbber.config.AppConfig
import io.r03el.photograbber.model.QueueItem
```

### Types

- Prefer `data class` for model classes
- Use `sealed class` or `enum class` for restricted hierarchies
- Use nullable types (`?`) instead of default values where null is meaningful
- Type inference is preferred: `val items = mutableListOf<Item>()`

### Error Handling

- Use `try-catch` for expected exceptions
- Log errors with context using SLF4J: `logger.error("message", exception)`
- Use `Result` or `runCatching` for functional error handling
- Always close resources with `.use { }` for AutoCloseable

```kotlin
try {
    processItem(item)
} catch (e: Exception) {
    logger.error("Failed to process file_id=${item.fileId}", e)
    handleFailure(item, e)
}
```

### Logging

- Use SLF4J LoggerFactory: `private val logger = LoggerFactory.getLogger(Class::class.java)`
- Log levels: `debug` for verbose, `info` for operations, `warn` for recoverable issues, `error` for failures
- Include context in log messages (file IDs, user IDs, etc.)

### Coroutines

- Use `Dispatchers.IO` for blocking operations (database, network)
- Use `SupervisorJob()` for independent child coroutines
- Prefer `async/await` for parallel operations
- Always cancel scopes properly in cleanup

```kotlin
private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

fun stop() {
    job?.cancel()
    scope.cancel()
}
```

### Database

- Use `.use { }` for automatic resource management
- Set `autoCommit = false` for transactions
- Always commit/rollback in try-finally blocks
- Use prepared statements with parameterized queries

### Dependency Injection

- Define modules in `AppModule.kt`
- Use `single { }` for singletons
- Use `get()` to retrieve dependencies
- Prefer constructor injection over field injection

### Configuration

- Environment variables for secrets (BOT_TOKEN, MINIO credentials)
- `application.yaml` for non-sensitive configuration
- Load config via `ConfigLoader` object

### Testing

- Test files go in `src/test/kotlin` mirroring main structure
- Use JUnit 5 (included via `kotlin-test-junit`)
- Test class names: `ClassNameTest`
- Test method names: descriptive with backticks

```kotlin
class QueueServiceTest {
    @Test
    fun `should enqueue new item successfully`() {
        // test code
    }
}
```

## Project Structure

```
src/main/kotlin/io/r03el/photograbber/
├── Application.kt          # Entry point
├── AppModule.kt            # Koin DI configuration
├── config/                 # Configuration classes
├── db/                     # Database connection & migrations
│   └── repository/         # Data access layer
├── model/                  # Data classes/enums
├── server/                 # HTTP server & controllers
└── service/                # Business logic
```

## Common Patterns

- **Repository Pattern**: Database access in `db/repository/`
- **Service Layer**: Business logic in `service/`
- **Data Classes**: Immutable models with copy()
- **Companion Objects**: Factory methods and static utilities
- **Extension Functions**: Add functionality to existing classes

## Security Notes

- Never commit secrets to repository
- Use environment variables for all credentials
- Validate all external inputs
- SQL injection prevention via prepared statements

## IDE Setup

- IntelliJ IDEA with Kotlin plugin
- Enable "Optimize imports on the fly"
- Set code style to Kotlin style guide
