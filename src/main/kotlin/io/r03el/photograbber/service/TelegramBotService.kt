package io.r03el.photograbber.service

import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.message
import com.github.kotlintelegrambot.entities.files.PhotoSize
import com.github.kotlintelegrambot.entities.files.Video
import com.github.kotlintelegrambot.entities.files.VideoNote
import io.r03el.photograbber.config.TelegramConfig
import io.r03el.photograbber.model.MediaFileMetadata
import io.r03el.photograbber.model.Type
import org.slf4j.LoggerFactory

class TelegramBotService(
    private val config: TelegramConfig,
    private val fileProcessor: FileProcessor,
) {
    private val logger = LoggerFactory.getLogger(TelegramBotService::class.java)

    fun start() {
        logger.info("Starting Telegram bot with config: groupsToMonitor=${config.groupsToMonitor}")

        val bot =
            bot {
                token = config.botToken

                dispatch {
                    message {
                        val message = this.message
                        val chatId = message.chat.id

                        logger.info("DEBUG: Received message in chat $chatId from user ${message.from?.id}")
                        logger.info("DEBUG: Message text: ${message.text}")
                        logger.info("DEBUG: Message has photo: ${message.photo != null}")
                        logger.info("DEBUG: Message has videoNote: ${message.videoNote != null}")

                        val text = message.text
                        if (text != null) {
                            handleText(text, chatId)
                        }
                        
                        if (!config.enableFilter && chatId !in config.groupsToMonitor) {
                            logger.info("DEBUG: Chat $chatId not in monitored groups, skipping")
                            return@message
                        }

                        val metadata =
                            MediaFileMetadata(
                                chatId = chatId,
                                userId = message.from?.id ?: 0L,
                                timestamp = System.currentTimeMillis()
                            )

                        val photos: List<PhotoSize>? = message.photo

                        if (photos != null) {
                            logger.info("DEBUG: Processing photo in chat $chatId")
                            handlePhoto(photos, metadata)
                        }

                        val videoNote: VideoNote? = message.videoNote
                        if (videoNote != null) {
                            logger.info("DEBUG: Processing video note in chat $chatId")
                            handleVideo(videoNote, metadata)
                        }


                        val video = message.video

                        if (video != null) {
                            logger.info("DEBUG: Processing video note in chat $chatId")
                            handleVideo(video, metadata)
                        }
                    }
                }
            }

        logger.info("Starting Telegram bot polling")
        bot.startPolling()
    }

    private suspend fun handleVideo(
        note: Video,
        metadata: MediaFileMetadata
    ) {
        val fileId = note.fileId
        metadata.apply { type = Type.VIDEO}
        saveMediaCatching(fileId, metadata)
    }

    private suspend fun handleVideo(
        note: VideoNote,
        metadata: MediaFileMetadata,
    ) {
        val fileId = note.fileId
        metadata.apply { type = Type.VIDEO_NOTE}

        logger.info(
            "Received video note from group ${metadata.chatId}, user ${metadata.userId}, file_id: $fileId",
        )

        saveMediaCatching(fileId, metadata)
    }

    private fun handleText(
        msg: String,
        chatId: Long,
    ) {
        logger.info("New message in $chatId")
    }

    private suspend fun handlePhoto(
        photos: List<PhotoSize>,
        metadata: MediaFileMetadata,
    ) {
        photos.maxBy { it.fileSize ?: 0 }.let { photo ->
            val fileId = photo.fileId
            metadata.apply { type = Type.IMAGE }
            logger.info(
                "Received photo from group ${metadata.chatId}, user ${metadata.userId}, file_id: $fileId",
            )

            saveMediaCatching(fileId, metadata)
        }
    }

    private suspend fun saveMediaCatching(
        fileId: String,
        metadata: MediaFileMetadata,
    ) {
        try {
            fileProcessor.processMedia(
                fileId = fileId,
                metadata,
            )
        } catch (e: Exception) {
            logger.error("Error processing photo", e)
        }
    }
}
