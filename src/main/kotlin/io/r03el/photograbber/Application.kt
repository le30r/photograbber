package io.r03el.photograbber

import io.r03el.photograbber.server.HttpServer
import io.r03el.photograbber.service.GalleryService
import io.r03el.photograbber.service.TelegramBotService
import kotlinx.coroutines.runBlocking
import org.koin.core.context.GlobalContext.startKoin
import org.koin.java.KoinJavaComponent.getKoin
import org.koin.logger.slf4jLogger
import java.util.concurrent.Executors

fun main() = runBlocking {
    println("DEBUG: Application starting...")
    startKoin {
        slf4jLogger()
        modules(appModule)
    }

    println("DEBUG: Koin initialized")

    val tpe = Executors.newSingleThreadExecutor()
    tpe.submit(getKoin().get<HttpServer>())

    val galleryService = getKoin().get<GalleryService>()
    galleryService.onStart()

    val botService: TelegramBotService = getKoin().get()
    println("DEBUG: Got TelegramBotService")
    botService.start()
}
