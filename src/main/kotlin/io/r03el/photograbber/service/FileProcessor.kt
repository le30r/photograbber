package io.r03el.photograbber.service

import com.github.kotlintelegrambot.Bot
import io.r03el.photograbber.model.MediaFileMetadata
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.ZoneId

class FileProcessor(
    private val bot: Bot,
    private val minioService: MinioService,
    private val galleryService: GalleryService,
) {
    private val logger = LoggerFactory.getLogger(FileProcessor::class.java)

    suspend fun processMediaFromQueue(
        fileId: String,
        metadata: MediaFileMetadata,
    ): String? {
        return try {
            val (response, _) = bot.getFile(fileId)
            val file = response?.body()?.result ?: return null

            val filePath = file.filePath ?: return null

            val (body, _) = bot.downloadFile(filePath)

            if (body == null) {
                return null
            }

            val timestamp = metadata.timestamp
            val date =
                Instant
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
                    type = type,
                    originalFileName = metadata.originalFileName,
                )

            val userId = metadata.userId
            val metadataMap =
                buildMap {
                    put("chat_id", chatId.toString())
                    put("user_id", userId.toString())
                    put("file_id", fileId)
                    put("timestamp", timestamp.toString())
                    put("date", date.toString())
                    put("type", type.toString())
                    metadata.originalFileName?.let { put("original_file_name", it) }
                }

            val uploadedFileName =
                body.body().use { responseBody ->
                    responseBody?.byteStream()?.use { input ->
                        minioService.uploadFile(input, fileName, metadataMap)
                    }
                }

            if (uploadedFileName == null) {
                return null
            }

            galleryService.saveImage(uploadedFileName, metadata)

            val originalNameLog = metadata.originalFileName?.let { ", original_name: $it" } ?: ""
            logger.info(
                "Successfully processed and uploaded media: $uploadedFileName " +
                    "(group: $chatId, user: $userId$originalNameLog)",
            )

            uploadedFileName
        } catch (e: Exception) {
            logger.error(
                "Failed to process media: group_id=${metadata.chatId}, user_id=${metadata.userId}, file_id=$fileId. Error: ${e.message}",
                e,
            )
            throw e
        }
    }
}
