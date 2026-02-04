package io.r03el.photograbber.server

import org.slf4j.LoggerFactory
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.Executors
import java.util.concurrent.ThreadPoolExecutor
import kotlin.math.log


class HttpServer(private val galleryController: GalleryController) : Runnable {
    private val logger = LoggerFactory.getLogger(HttpServer::class.java)
    private val threadPoolExecutor = Executors.newCachedThreadPool()

    @Volatile
    var isRunning: Boolean = true

    override fun run() {
        Runtime.getRuntime().addShutdownHook(object : Thread() {
            override fun run() {
                logger.info("Stopping server...")
                isRunning = false
            }
        })

        val serverSocket: ServerSocket = ServerSocket(8080)
        logger.info("Server started on port 8080")

        while (isRunning) {
            val clientSocket: Socket = serverSocket.accept()
            handle(clientSocket)
        }
    }

    private fun handle(clientSocket: Socket) {

        val out = PrintWriter(clientSocket.getOutputStream(), true)
        logger.info("New request")

        val body = galleryController.loadGallery()
        val bytes = body.toByteArray(Charsets.UTF_8)

        // Send HTTP headers and HTML content
        out.println("HTTP/1.1 200 OK")
        out.println("Content-Type: text/html")
        out.println("Content-Length: ${bytes.size}")
        out.println("\r\n")
        out.write(body)
        out.close()
        out.flush()
        clientSocket.close()
    }


}