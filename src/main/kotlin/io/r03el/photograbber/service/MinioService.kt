package io.r03el.photograbber.service

import io.minio.*
import io.minio.errors.*
import io.r03el.photograbber.config.MinioConfig
import io.r03el.photograbber.model.MediaFileMetadata
import io.r03el.photograbber.model.Type
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import java.io.InputStream
import java.time.LocalDate

class MinioService(
    private val config: MinioConfig,
) {
    private val logger = LoggerFactory.getLogger(MinioService::class.java)
    private val minioClient: MinioAsyncClient = MinioAsyncClient
        .builder()
        .endpoint(config.endpoint)
        .credentials(config.accessKey, config.secretKey)
        .build()

    init {
        runBlocking {
            ensureBucketExists()
        }
    }

    private suspend fun ensureBucketExists() {
        try {
            if (!(minioClient.bucketExists(BucketExistsArgs.builder().bucket(config.bucket).build())).await()) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(config.bucket).build())
                logger.info("Created bucket: ${config.bucket}")
            } else {
                logger.info("Bucket already exists: ${config.bucket}")
            }
        } catch (e: ErrorResponseException) {
            logger.error("Error checking/creating bucket: ${e.message}", e)
            throw e
        } catch (e: Exception) {
            logger.error("Unexpected error ensuring bucket exists: ${e.message}", e)
            throw e
        }
    }

    suspend fun uploadFile(
        inputStream: InputStream,
        fileName: String,
        metadata: Map<String, String>,
    ): String =
        try {
            val args =
                PutObjectArgs
                    .builder()
                    .bucket(config.bucket)
                    .`object`(fileName)
                    .stream(inputStream, inputStream.available().toLong(), -1)
                    .userMetadata(metadata)
                    .build()

            minioClient.putObject(args).await()
            logger.info("Successfully uploaded file: $fileName")
            "${config.bucket}/$fileName"
        } catch (e: ErrorResponseException) {
            logger.error("Error uploading file $fileName: ${e.message}", e)
            throw e
        } catch (e: Exception) {
            logger.error("Unexpected error uploading file $fileName: ${e.message}", e)
            throw e
        } finally {
            inputStream.close()
        }


    fun listFiles() = try {
        val args =
            ListObjectsArgs
                .builder()
                .recursive(true)
                .bucket(config.bucket)
                .build()

        minioClient.listObjects(args).map { obj -> "${config.bucket}/${obj.get().objectName()}" }

    } catch (e: ErrorResponseException) {
        logger.error("Error listing files in ${config.bucket} ${e.message}", e)
        throw e
    } catch (e: Exception) {
        logger.error("Unexpected error listing files in ${config.bucket}: ${e.message}", e)
        throw e
    }

    fun generateFileName(
        groupId: Long,
        date: LocalDate,
        timestamp: Long,
        fileId: String,
        type: Type,
    ): String = "$groupId/$date/${timestamp}_$fileId.${type.extension}"
}
