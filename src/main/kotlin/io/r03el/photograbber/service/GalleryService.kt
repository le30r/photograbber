package io.r03el.photograbber.service

import io.r03el.photograbber.config.MinioConfig
import io.r03el.photograbber.model.MediaFileMetadata
import io.r03el.photograbber.model.Type
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentSkipListSet

class GalleryService(
    private val minioService: MinioService,
    private val minioConfig: MinioConfig
) {
    private val imageCache: ConcurrentSkipListSet<String> = ConcurrentSkipListSet<String>()

    fun getImages(): List<String> {
        return imageCache.map { it -> "${minioConfig.endpoint}/${it}"}
    }

    fun saveImage(imagePath: String, metadata: MediaFileMetadata) {
        imageCache.add(imagePath)
    }

    fun onStart() {
        imageCache.addAll(minioService.listFiles())
    }
}