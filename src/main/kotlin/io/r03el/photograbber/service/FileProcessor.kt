package io.r03el.photograbber.service

import com.github.kotlintelegrambot.Bot
import io.r03el.photograbber.model.MediaFileMetadata
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.ZoneId

class FileProcessor(
    private val bot: Bot,
    private val minioService: MinioService,
    private val galleryService: GalleryService
) {
    private val logger = LoggerFactory.getLogger(FileProcessor::class.java)

    suspend fun processMedia(
        fileId: String,
        metadata: MediaFileMetadata
    ) {
        try {

            val (response, _) = bot.getFile(fileId)
            val file = response?.body()?.result ?: return

            val filePath = file.filePath ?: return

            val (body, _) = bot.downloadFile(filePath)

            if (body == null) {
                return
            }

            val timestamp = metadata.timestamp
            val date = Instant
                .ofEpochMilli(timestamp)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()

            val chatId = metadata.chatId
            val type = metadata.type
            val fileName =
                minioService.generateFileName(
                    groupId = chatId,
                    date = date,
                    timestamp = timestamp,
                    fileId = fileId,
                    type = type
                )

            val userId = metadata.userId
            val metadataMap =
                mapOf(
                    "group_id" to chatId.toString(),
                    "user_id" to userId.toString(),
                    "file_id" to fileId,
                    "timestamp" to timestamp.toString(),
                    "date" to date.toString(),
                    "type" to type.toString()
                )

            val uploadedFileName =  body.body().use { responseBody ->
                responseBody?.byteStream()?.use { input ->
                    return@use minioService.uploadFile(input, fileName, metadataMap)
                }
            }

            if (uploadedFileName == null) {
                return
            }

            galleryService.saveImage(uploadedFileName, metadata)

            logger.info(
                "Successfully processed and uploaded media: $uploadedFileName " +
                        "(, group: $chatId, user: $userId)",
            )
        } catch (e: Exception) {
            logger.error(
                "Failed to process media: group_id=${metadata.chatId}, user_id=${metadata.userId}, file_id=$fileId. Error: ${e.message}",
                e,
            )
            throw e
        }
    }
}
