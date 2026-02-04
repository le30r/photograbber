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

        val telegramConfig = TelegramConfig(
            botToken = botToken,
            enableFilter = telegramMap["enableFilter"] as Boolean,
            groupsToMonitor = (telegramMap["groupsToMonitor"] as List<*>)
                .map { it.toString().toLong() }
        )

        val minioConfig = MinioConfig(
            endpoint = System.getenv("MINIO_ENDPOINT") ?: error("MINIO_ENDPOINT is required"),
            accessKey = System.getenv("MINIO_ACCESS_KEY") ?: error("MINIO_ACCESS_KEY is required"),
            secretKey = System.getenv("MINIO_SECRET_KEY") ?: error("MINIO_SECRET_KEY is required"),
            bucket = System.getenv("MINIO_BUCKET") ?: error("MINIO_BUCKET is required"),
        )

        return AppConfig(
            telegram = telegramConfig,
            minio = minioConfig
        )
    }
}
