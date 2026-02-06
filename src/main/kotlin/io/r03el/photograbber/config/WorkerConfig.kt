package io.r03el.photograbber.config

class WorkerConfig(
    val enabled: Boolean = true,
    val concurrency: Int = 3,
    val pollIntervalMs: Long = 1000,
    val maxRetries: Int = 3,
    val retryBaseDelayMs: Long = 5000,
)
