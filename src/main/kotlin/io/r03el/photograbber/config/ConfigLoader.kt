package io.r03el.photograbber.config

import org.yaml.snakeyaml.Yaml

object ConfigLoader {
    fun load(): AppConfig {
        val input =
            this::class.java.classLoader
                .getResourceAsStream("application.yaml")
                ?: error("application.yaml not found")

        val yaml = Yaml()
        val root = yaml.load<Map<String, Any>>(input)

        val telegramMap = root["telegram"] as Map<*, *>

        val botToken =
            System.getenv("BOT_TOKEN")
                ?: error("BOT_TOKEN is required")

        val galleryUrl =
            System.getenv("GALLERY_URL")
                ?: error("GALLERY_URL is required")

        val telegramConfig =
            TelegramConfig(
                botToken = botToken,
                enableFilter = telegramMap["enableFilter"] as Boolean,
                groupsToMonitor =
                    (telegramMap["groupsToMonitor"] as List<*>)
                        .map { it.toString().toLong() },
                gallery = galleryUrl
            )

        val minioConfig =
            MinioConfig(
                endpoint = System.getenv("MINIO_ENDPOINT") ?: error("MINIO_ENDPOINT is required"),
                accessKey = System.getenv("MINIO_ACCESS_KEY") ?: error("MINIO_ACCESS_KEY is required"),
                secretKey = System.getenv("MINIO_SECRET_KEY") ?: error("MINIO_SECRET_KEY is required"),
                bucket = System.getenv("MINIO_BUCKET") ?: error("MINIO_BUCKET is required"),
            )

        val queueMap = root["queue"] as? Map<*, *>
        val queueConfig =
            if (queueMap != null) {
                val sqliteMap = queueMap["sqlite"] as? Map<*, *>
                QueueConfig(
                    sqlitePath = sqliteMap?.get("path") as? String ?: "./data/queue.db",
                )
            } else {
                QueueConfig()
            }

        val workerMap = root["worker"] as? Map<*, *>
        val workerConfig =
            if (workerMap != null) {
                WorkerConfig(
                    enabled = workerMap["enabled"] as? Boolean ?: true,
                    concurrency = (workerMap["concurrency"] as? Number)?.toInt() ?: 3,
                    pollIntervalMs = (workerMap["pollIntervalMs"] as? Number)?.toLong() ?: 1000,
                    maxRetries = (workerMap["maxRetries"] as? Number)?.toInt() ?: 3,
                    retryBaseDelayMs = (workerMap["retryBaseDelayMs"] as? Number)?.toLong() ?: 5000,
                )
            } else {
                WorkerConfig()
            }

        return AppConfig(
            telegram = telegramConfig,
            minio = minioConfig,
            queue = queueConfig,
            worker = workerConfig,
        )
    }
}
