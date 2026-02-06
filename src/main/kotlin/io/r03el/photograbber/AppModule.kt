package io.r03el.photograbber

import com.github.kotlintelegrambot.bot
import io.r03el.photograbber.config.AppConfig
import io.r03el.photograbber.config.ConfigLoader
import io.r03el.photograbber.config.QueueConfig
import io.r03el.photograbber.config.TelegramConfig
import io.r03el.photograbber.config.WorkerConfig
import io.r03el.photograbber.db.DatabaseConnection
import io.r03el.photograbber.db.MigrationRunner
import io.r03el.photograbber.db.repository.UploadQueueRepository
import io.r03el.photograbber.server.GalleryController
import io.r03el.photograbber.server.HttpServer
import io.r03el.photograbber.service.FileProcessor
import io.r03el.photograbber.service.GalleryService
import io.r03el.photograbber.service.MinioService
import io.r03el.photograbber.service.QueueService
import io.r03el.photograbber.service.TelegramBotService
import io.r03el.photograbber.service.WorkerService
import org.koin.dsl.module
import java.util.concurrent.Executors
import java.util.concurrent.ThreadPoolExecutor

val appModule =
    module {

        single {
            ConfigLoader.load()
        }

        single {
            get<AppConfig>().telegram
        }

        single {
            get<AppConfig>().minio
        }

        single {
            get<AppConfig>().queue
        }

        single {
            get<AppConfig>().worker
        }

        single {
            GalleryService(get(), get())
        }

        single {
            GalleryController(get())
        }

        single {
            HttpServer(get())
        }

        single {
            MinioService(get())
        }

        single {
            val config = get<TelegramConfig>()
            bot {
                token = config.botToken
            }
        }

        single {
            FileProcessor(
                bot = get(),
                minioService = get(),
                galleryService = get(),
            )
        }

        single {
            DatabaseConnection(get())
        }

        single {
            MigrationRunner(get()).apply { runMigrations() }
        }

        single {
            UploadQueueRepository(get())
        }

        single {
            QueueService(get())
        }

        single {
            WorkerService(get(), get(), get())
        }

        single {
            TelegramBotService(
                config = get(),
                queueService = get(),
            )
        }
    }
