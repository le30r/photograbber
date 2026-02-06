package io.r03el.photograbber.model

data class MediaFileMetadata(
    val chatId: Long,
    val userId: Long,
    val timestamp: Long,
    val type: Type,
    val originalFileName: String? = null,
) {
    companion object {
        fun create(
            chatId: Long,
            userId: Long,
            timestamp: Long = System.currentTimeMillis(),
            type: Type,
            originalFileName: String? = null,
        ): MediaFileMetadata =
            MediaFileMetadata(
                chatId = chatId,
                userId = userId,
                timestamp = timestamp,
                type = type,
                originalFileName = originalFileName,
            )

        fun fromMinioMetadata(metadata: Map<String, String>?): MediaFileMetadata? {
            if (metadata == null) return null

            val chatId = metadata["chat_id"]?.toLongOrNull()
            val userId = metadata["user_id"]?.toLongOrNull()
            val timestamp = metadata["timestamp"]?.toLongOrNull()
            val type = metadata["type"]?.let { Type.tryParse(it) }
            val originalFileName = metadata["original_file_name"]

            return if (chatId != null && userId != null && timestamp != null && type != null) {
                MediaFileMetadata(
                    chatId = chatId,
                    userId = userId,
                    timestamp = timestamp,
                    type = type,
                    originalFileName = originalFileName,
                )
            } else {
                null
            }
        }
    }
}

enum class Type(
    val extension: String,
) {
    IMAGE("jpg"),
    VIDEO("mp4"),
    VIDEO_NOTE("mp4"),
    DOCUMENT_IMAGE("jpg"),
    DOCUMENT_VIDEO("mp4"),
    ;

    companion object {
        fun tryParse(str: String?) = str?.let { Type.entries.toTypedArray().findLast { str == it.name } } ?: IMAGE

        fun fromMimeType(
            mimeType: String?,
            fileName: String?,
        ): Type? {
            // Сначала проверяем MIME-type
            if (mimeType != null) {
                when {
                    mimeType.startsWith("image/") -> return DOCUMENT_IMAGE
                    mimeType.startsWith("video/") -> return DOCUMENT_VIDEO
                }
            }

            // Фоллбек на расширение файла
            val ext = fileName?.substringAfterLast('.', "")?.lowercase()
            return when (ext) {
                "jpg", "jpeg", "png", "gif", "webp", "bmp", "tiff", "heic", "heif" -> DOCUMENT_IMAGE
                "mp4", "mov", "avi", "mkv", "webm", "flv", "wmv", "m4v", "3gp" -> DOCUMENT_VIDEO
                else -> null
            }
        }
    }
}
