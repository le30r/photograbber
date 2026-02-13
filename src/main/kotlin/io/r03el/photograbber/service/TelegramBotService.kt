package io.r03el.photograbber.service

import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.command
import com.github.kotlintelegrambot.dispatcher.message
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.InlineKeyboardMarkup
import com.github.kotlintelegrambot.entities.ParseMode
import com.github.kotlintelegrambot.entities.files.Document
import com.github.kotlintelegrambot.entities.files.PhotoSize
import com.github.kotlintelegrambot.entities.files.Video
import com.github.kotlintelegrambot.entities.files.VideoNote
import com.github.kotlintelegrambot.entities.keyboard.InlineKeyboardButton
import io.r03el.photograbber.config.TelegramConfig
import io.r03el.photograbber.model.MediaFileMetadata
import io.r03el.photograbber.model.QueueItem
import io.r03el.photograbber.model.Type
import org.slf4j.LoggerFactory

class TelegramBotService(
    private val config: TelegramConfig,
    private val queueService: QueueService,
) {
    private val logger = LoggerFactory.getLogger(TelegramBotService::class.java)
    private lateinit var bot: com.github.kotlintelegrambot.Bot

    private val inlineKeyboardMarkup = InlineKeyboardMarkup.create(
        listOf(InlineKeyboardButton.Url(text = "\uD83D\uDDBC Галерея", url = config.gallery)),
    )

    fun start() {
        logger.info("Starting Telegram bot with config: groupsToMonitor=${config.groupsToMonitor}")

        bot =
            bot {
                token = config.botToken

                dispatch {
                    command("status") {
                        val chatId = message.chat.id
                        logger.info("DEBUG: Received status command from $chatId")
                        sendStatusMessage(chatId)
                    }

                    command("gallery") {
                        val chatId = message.chat.id
                        sendGallery(chatId)
                    }

                    command("start") {
                        val chatId = message.chat.id

                        bot.sendMessage(
                            chatId = ChatId.fromId(chatId),
                            text = """
                                Добро пожаловать!

                                Этот бот создан для сбора фотографий со свадьбы.
                                Пожалуйста, отправьте сюда снимки с мероприятия — они будут добавлены в общую галерею.

                                Спасибо, что помогаете сохранить важные моменты этого дня 🤍
                            """.trimIndent(),
                            replyMarkup = inlineKeyboardMarkup
                        )
                    }

                    message {
                        val message = this.message
                        val chatId = message.chat.id

                        logger.info("DEBUG: Received message in chat $chatId from user ${message.from?.id}")
                        logger.info("DEBUG: Message text: ${message.text}")
                        logger.info("DEBUG: Message has photo: ${message.photo != null}")
                        logger.info("DEBUG: Message has videoNote: ${message.videoNote != null}")
                        logger.info("DEBUG: Message has video: ${message.video != null}")
                        logger.info("DEBUG: Message has document: ${message.document != null}")

                        val text = message.text
                        if (text != null) {
                            handleText(text, chatId)
                        }

                        if (config.enableFilter && chatId !in config.groupsToMonitor) {
                            logger.info("DEBUG: Chat $chatId not in monitored groups, skipping")
                            return@message
                        }

                        val baseMetadata =
                            MediaFileMetadata.create(
                                chatId = chatId,
                                userId = message.from?.id ?: 0L,
                                type = Type.IMAGE, // default, will be overridden
                            )

                        val photos: List<PhotoSize>? = message.photo

                        if (photos != null) {
                            logger.info("DEBUG: Processing photo in chat $chatId")
                            handlePhoto(photos, baseMetadata)
                            bot.sendMessage(
                                ChatId.fromId(chatId),
                                "Спасибо! Ваше фото добавлено в очередь ✅",
                                replyMarkup = inlineKeyboardMarkup
                            )
                        }

                        val videoNote: VideoNote? = message.videoNote
                        if (videoNote != null) {
                            logger.info("DEBUG: Processing video note in chat $chatId")
                            handleVideo(videoNote, baseMetadata)
                            bot.sendMessage(
                                ChatId.fromId(chatId),
                                "Спасибо! Ваш кружок добавлен в очередь ✅",
                                replyMarkup = inlineKeyboardMarkup
                            )
                        }

                        val video = message.video

                        if (video != null) {
                            logger.info("DEBUG: Processing video in chat $chatId")
                            handleVideo(video, baseMetadata)
                            bot.sendMessage(
                                ChatId.fromId(chatId),
                                "Спасибо! Ваше видео добавлен в очередь ✅",
                                replyMarkup = inlineKeyboardMarkup
                            )
                        }

                        val document = message.document
                        if (document != null) {
                            logger.info("DEBUG: Processing document in chat $chatId")
                            handleDocument(document, baseMetadata)
                            bot.sendMessage(
                                ChatId.fromId(chatId),
                                "Спасибо! Ваше вложение добавлен в очередь ✅",
                                replyMarkup = inlineKeyboardMarkup
                            )
                        }

                    }
                }
            }

        logger.info("Starting Telegram bot polling")
        bot.startPolling()
    }

    private fun handleVideo(
        video: Video,
        baseMetadata: MediaFileMetadata,
    ) {
        val fileId = video.fileId
        val metadata = baseMetadata.copy(type = Type.VIDEO)
        saveMediaCatching(fileId, metadata)
    }

    private fun handleVideo(
        note: VideoNote,
        baseMetadata: MediaFileMetadata,
    ) {
        val fileId = note.fileId
        val metadata = baseMetadata.copy(type = Type.VIDEO_NOTE)

        logger.info(
            "Received video note from group ${metadata.chatId}, user ${metadata.userId}, file_id: $fileId",
        )

        saveMediaCatching(fileId, metadata)
    }

    private fun handleDocument(
        document: Document,
        baseMetadata: MediaFileMetadata,
    ) {
        val fileId = document.fileId
        val fileName = document.fileName
        val mimeType = document.mimeType
        val fileSize = document.fileSize

        // Определяем тип документа по MIME-type или расширению
        val documentType = Type.fromMimeType(mimeType, fileName)

        if (documentType == null) {
            logger.info(
                "Skipping document from group ${baseMetadata.chatId}: " +
                        "mimeType=$mimeType, fileName=$fileName - not a photo or video",
            )
            return
        }

        val metadata =
            baseMetadata.copy(
                type = documentType,
                originalFileName = fileName,
            )

        logger.info(
            "Received document from group ${metadata.chatId}, user ${metadata.userId}, " +
                    "file_id: $fileId, type: $documentType, mimeType: $mimeType, fileName: $fileName, size: $fileSize",
        )

        saveMediaCatching(fileId, metadata)
    }

    private fun handleText(
        @Suppress("UNUSED_PARAMETER") msg: String,
        chatId: Long,
    ) {
        logger.info("New message in $chatId")
    }

    private fun sendStatusMessage(chatId: Long) {
        val queueStats =
            try {
                queueService.getQueueStats()
            } catch (e: Exception) {
                logger.error("Failed to get queue stats", e)
                emptyMap<String, Int>()
            }
        val completedCount =
            try {
                queueService.getCompletedCount()
            } catch (e: Exception) {
                logger.error("Failed to get completed count", e)
                0
            }
        val lastUploadTime =
            try {
                queueService.getLastUploadTime()
            } catch (e: Exception) {
                logger.error("Failed to get last upload time", e)
                null
            }
        val avgProcessingTime =
            try {
                queueService.getAverageProcessingTime()
            } catch (e: Exception) {
                logger.error("Failed to get average processing time", e)
                null
            }
        val mediaTypeStats =
            try {
                queueService.getMediaTypeStats()
            } catch (e: Exception) {
                logger.error("Failed to get media type stats", e)
                emptyMap<String, Int>()
            }

        val pendingCount = queueStats["PENDING"] ?: 0
        val processingCount = queueStats["PROCESSING"] ?: 0
        val failedCount = queueStats["FAILED"] ?: 0

        val lastUploadText =
            lastUploadTime?.let {
                val duration = System.currentTimeMillis() - it
                val minutes = duration / 60000
                val hours = minutes / 60
                val days = hours / 24
                when {
                    days > 0 -> "$days дн. назад"
                    hours > 0 -> "$hours ч. назад"
                    minutes > 0 -> "$minutes мин. назад"
                    else -> "только что"
                }
            } ?: "нет данных"

        val avgTimeText =
            avgProcessingTime?.let {
                val seconds = (it / 1000).toInt()
                if (seconds < 60) {
                    "$seconds сек"
                } else {
                    "${seconds / 60} мин ${seconds % 60} сек"
                }
            } ?: "нет данных"

        val mediaBreakdown =
            buildString {
                if (mediaTypeStats.isNotEmpty()) {
                    append("\n📊 *По типам медиа:*\n")
                    mediaTypeStats.forEach { (type, count) ->
                        val emoji =
                            when (type) {
                                "IMAGE" -> "🖼️"
                                "VIDEO" -> "🎬"
                                "VIDEO_NOTE" -> "📹"
                                else -> "📄"
                            }
                        val typeName =
                            when (type) {
                                "IMAGE" -> "Фото"
                                "VIDEO" -> "Видео"
                                "VIDEO_NOTE" -> "Видео-сообщения"
                                "DOCUMENT_IMAGE" -> "Фото (документ)"
                                "DOCUMENT_VIDEO" -> "Видео (документ)"
                                else -> type
                            }
                        append("$emoji $typeName: $count\n")
                    }
                }
            }

        val message =
            """
            📈 *Статус PhotoGrabber*
            
            ✅ *Загружено:* $completedCount файлов
            🕐 *Последняя загрузка:* $lastUploadText
            ⏱️ *Среднее время обработки:* $avgTimeText
            
            📋 *Текущая очередь:*
            ⏳ В ожидании: $pendingCount
            🔄 Обрабатывается: $processingCount
            ❌ Ошибки: $failedCount
            $mediaBreakdown
            """.trimIndent()

        try {
            bot.sendMessage(
                chatId = ChatId.fromId(chatId),
                text = message,
                parseMode = ParseMode.MARKDOWN,
            )
            logger.info("Sent status message to chat $chatId")
        } catch (e: Exception) {
            logger.error("Error sending status message to chat $chatId", e)
        }
    }

    private suspend fun handlePhoto(
        photos: List<PhotoSize>,
        baseMetadata: MediaFileMetadata,
    ) {
        photos.maxBy { it.fileSize ?: 0 }.let { photo ->
            val fileId = photo.fileId
            val metadata = baseMetadata.copy(type = Type.IMAGE)
            logger.info(
                "Received photo from group ${metadata.chatId}, user ${metadata.userId}, file_id: $fileId",
            )

            saveMediaCatching(fileId, metadata)
        }
    }

    private fun saveMediaCatching(
        fileId: String,
        metadata: MediaFileMetadata,
    ) {
        try {
            val queueItem =
                QueueItem(
                    fileId = fileId,
                    chatId = metadata.chatId,
                    userId = metadata.userId,
                    timestamp = metadata.timestamp,
                    mediaType = metadata.type,
                    originalFileName = metadata.originalFileName,
                )

            queueService.enqueue(queueItem)
        } catch (e: Exception) {
            logger.error("Error enqueueing media", e)
        }
    }


    private fun sendGallery(
        chatId: Long
    ) {
        bot.sendMessage(ChatId.fromId(chatId), "Галерея доступна по ссылке: ${config.gallery}")
    }
}


