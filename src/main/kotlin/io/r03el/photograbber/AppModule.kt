package io.r03el.photograbber

import com.github.kotlintelegrambot.bot
import io.r03el.photograbber.config.AppConfig
import io.r03el.photograbber.config.ConfigLoader
import io.r03el.photograbber.config.TelegramConfig
import io.r03el.photograbber.server.GalleryController
import io.r03el.photograbber.server.HttpServer
import io.r03el.photograbber.service.FileProcessor
import io.r03el.photograbber.service.GalleryService
import io.r03el.photograbber.service.MinioService
import io.r03el.photograbber.service.TelegramBotService
import org.koin.dsl.module
import java.util.concurrent.Executors
import java.util.concurrent.ThreadPoolExecutor

val appModule = module {

    single {
        ConfigLoader.load()
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
        get<AppConfig>().telegram
    }

    single {
        get<AppConfig>().minio
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
            galleryService = get()
        )
    }

    single {
        TelegramBotService(
            config = get(),
            fileProcessor = get()
        )
    }
}
