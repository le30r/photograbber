package io.r03el.photograbber.model

data class MediaFileMetadata(
    val chatId: Long,
    val userId: Long,
    val timestamp: Long,

    ) {
    lateinit var type: Type
}

enum class Type(
    val extension: String
) {
    IMAGE("jpg"),
    VIDEO("mp4"),
    VIDEO_NOTE("mp4");

    companion object {
        fun tryParse(str: String?) = str?.let { Type.entries.toTypedArray().findLast { str == it.name } } ?: IMAGE
    }
}