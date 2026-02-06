package io.r03el.photograbber.service

import io.r03el.photograbber.config.WorkerConfig
import io.r03el.photograbber.db.repository.UploadQueueRepository
import io.r03el.photograbber.model.MediaFileMetadata
import io.r03el.photograbber.model.QueueItem
import io.r03el.photograbber.model.QueueStatus
import io.r03el.photograbber.model.Type
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import kotlin.math.pow

class WorkerService(
    private val config: WorkerConfig,
    private val repository: UploadQueueRepository,
    private val fileProcessor: FileProcessor,
) {
    private val logger = LoggerFactory.getLogger(WorkerService::class.java)
    private var job: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun start() {
        if (!config.enabled) {
            logger.info("Worker is disabled in configuration")
            return
        }

        logger.info("Starting WorkerService with concurrency=${config.concurrency}, pollInterval=${config.pollIntervalMs}ms")

        job =
            scope.launch {
                while (isActive) {
                    try {
                        processBatch()
                    } catch (e: Exception) {
                        logger.error("Error in worker loop", e)
                    }
                    delay(config.pollIntervalMs)
                }
            }
    }

    fun stop() {
        logger.info("Stopping WorkerService")
        job?.cancel()
        scope.cancel()
    }

    private suspend fun processBatch() {
        val items = repository.getPendingItems(config.concurrency)

        if (items.isNotEmpty()) {
            logger.debug("Processing batch of ${items.size} items")
        }

        items
            .map { item ->
                scope.async {
                    processItem(item)
                }
            }.awaitAll()
    }

    private suspend fun processItem(item: QueueItem) {
        val claimed = repository.markAsProcessing(item.id!!)

        if (!claimed) {
            logger.debug("Failed to claim item ${item.id}, probably already processed")
            return
        }

        logger.info("Processing file_id=${item.fileId}, attempt ${item.retryCount + 1}")

        try {
            val metadata =
                MediaFileMetadata.create(
                    chatId = item.chatId,
                    userId = item.userId,
                    timestamp = item.timestamp,
                    type = item.mediaType,
                    originalFileName = item.originalFileName
                )

            val minioPath = fileProcessor.processMediaFromQueue(item.fileId, metadata)

            if (minioPath != null) {
                repository.markAsCompleted(item.id, minioPath)
                logger.info("Successfully processed file_id=${item.fileId}, path=$minioPath")
            } else {
                throw IllegalStateException("Upload returned null path")
            }
        } catch (e: Exception) {
            logger.error("Failed to process file_id=${item.fileId}", e)
            handleFailure(item, e)
        }
    }

    private fun handleFailure(
        item: QueueItem,
        error: Exception,
    ) {
        val shouldRetry = item.retryCount < config.maxRetries

        if (shouldRetry) {
            val delayMs = config.retryBaseDelayMs * (2.0.pow(item.retryCount.toDouble())).toLong()
            logger.warn("Will retry file_id=${item.fileId} after ${delayMs}ms (attempt ${item.retryCount + 1}/${config.maxRetries})")
        } else {
            logger.error("Permanently failed file_id=${item.fileId} after ${config.maxRetries} attempts")
        }

        repository.markAsFailed(item.id!!, error.message ?: "Unknown error", config.maxRetries)
    }

    fun retryFailed() {
        logger.info("Resetting failed items with retry_count < ${config.maxRetries} to PENDING")
        repository.resetFailedToPending(config.maxRetries)
    }
}
