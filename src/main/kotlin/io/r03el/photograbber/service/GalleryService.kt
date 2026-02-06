package io.r03el.photograbber.service

import io.r03el.photograbber.config.MinioConfig
import io.r03el.photograbber.model.MediaFileMetadata
import io.r03el.photograbber.model.Type
import java.util.concurrent.ConcurrentHashMap

data class GalleryItem(
    val path: String,
    val metadata: MediaFileMetadata?,
    val url: String,
)

class GalleryService(
    private val minioService: MinioService,
    private val minioConfig: MinioConfig,
) {
    private val galleryCache: ConcurrentHashMap<String, GalleryItem> = ConcurrentHashMap()

    fun getItems(): List<GalleryItem> =
        galleryCache.values
            .sortedByDescending { it.metadata?.timestamp ?: 0L }

    fun saveImage(
        imagePath: String,
        metadata: MediaFileMetadata,
    ) {
        val item =
            GalleryItem(
                path = imagePath,
                metadata = metadata,
                url = "${minioConfig.endpoint}/$imagePath",
            )
        galleryCache[imagePath] = item
    }

    fun onStart() {
        val files = minioService.listFiles()
        files.forEach { filePath ->
            val objectName = filePath.removePrefix("${minioConfig.bucket}/")
            val minioMetadata = minioService.getObjectMetadata(objectName)
            val metadata = MediaFileMetadata.fromMinioMetadata(minioMetadata)

            val item =
                GalleryItem(
                    path = filePath,
                    metadata = metadata,
                    url = "${minioConfig.endpoint}/$filePath",
                )
            galleryCache[filePath] = item
        }
    }
}
