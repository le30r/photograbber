package io.r03el.photograbber

import io.r03el.photograbber.db.MigrationRunner
import io.r03el.photograbber.server.HttpServer
import io.r03el.photograbber.service.GalleryService
import io.r03el.photograbber.service.TelegramBotService
import io.r03el.photograbber.service.WorkerService
import kotlinx.coroutines.runBlocking
import org.koin.core.context.GlobalContext.startKoin
import org.koin.java.KoinJavaComponent.getKoin
import org.koin.logger.slf4jLogger
import java.util.concurrent.Executors

fun main() =
    runBlocking {
        println("DEBUG: Application starting...")
        startKoin {
            slf4jLogger()
            modules(appModule)
        }

        println("DEBUG: Koin initialized")

        // Run database migrations
        getKoin().get<MigrationRunner>()
        println("DEBUG: Database migrations completed")

        val tpe = Executors.newSingleThreadExecutor()
        tpe.submit(getKoin().get<HttpServer>())

        val galleryService = getKoin().get<GalleryService>()
        galleryService.onStart()

        // Start worker service
        val workerService = getKoin().get<WorkerService>()
        workerService.start()
        println("DEBUG: WorkerService started")

        val botService: TelegramBotService = getKoin().get()
        println("DEBUG: Got TelegramBotService")
        botService.start()
    }
