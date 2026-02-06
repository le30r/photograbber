package io.r03el.photograbber.model

data class QueueItem(
    val id: Long? = null,
    val fileId: String,
    val chatId: Long,
    val userId: Long,
    val timestamp: Long,
    val mediaType: Type,
    val status: QueueStatus = QueueStatus.PENDING,
    val retryCount: Int = 0,
    val errorMessage: String? = null,
    val originalFileName: String? = null,
    val createdAt: Long? = null,
    val updatedAt: Long? = null,
)
