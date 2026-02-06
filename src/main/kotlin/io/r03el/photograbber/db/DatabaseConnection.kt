package io.r03el.photograbber.db

import io.r03el.photograbber.config.QueueConfig
import java.sql.Connection
import java.sql.DriverManager

class DatabaseConnection(
    private val config: QueueConfig,
) {
    init {
        Class.forName("org.sqlite.JDBC")

        val dbFile = java.io.File(config.sqlitePath)
        dbFile.parentFile?.mkdirs()
    }

    fun getConnection(): Connection =
        DriverManager.getConnection("jdbc:sqlite:${config.sqlitePath}").apply {
            createStatement().execute("PRAGMA foreign_keys = ON")
        }
}
