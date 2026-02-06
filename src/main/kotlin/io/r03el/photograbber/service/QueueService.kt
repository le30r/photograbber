package io.r03el.photograbber.service

import io.r03el.photograbber.db.repository.UploadQueueRepository
import io.r03el.photograbber.model.QueueItem
import io.r03el.photograbber.model.QueueStatus
import org.slf4j.LoggerFactory

class QueueService(
    private val repository: UploadQueueRepository,
) {
    private val logger = LoggerFactory.getLogger(QueueService::class.java)

    fun enqueue(item: QueueItem): Boolean {
        logger.info("Enqueuing file_id=${item.fileId}, chat_id=${item.chatId}, type=${item.mediaType}")

        val success = repository.enqueue(item)

        if (success) {
            logger.info("Successfully enqueued file_id=${item.fileId}")
        } else {
            logger.info("File_id=${item.fileId} already exists in queue, skipping")
        }

        return success
    }

    fun getQueueStats(): Map<String, Int> = repository.getQueueStats()

    fun getCompletedCount(): Int = repository.getCompletedCount()

    fun getLastUploadTime(): Long? = repository.getLastUploadTime()

    fun getAverageProcessingTime(): Double? = repository.getAverageProcessingTime()

    fun getMediaTypeStats(): Map<String, Int> = repository.getMediaTypeStats()
}
