package io.r03el.photograbber.db

import java.sql.Connection

class MigrationRunner(
    private val dbConnection: DatabaseConnection,
) {
    fun runMigrations() {
        dbConnection.getConnection().use { conn ->
            createMigrationsTable(conn)

            val appliedMigrations = getAppliedMigrations(conn)

            if (!appliedMigrations.contains(1)) {
                migration001(conn)
                recordMigration(conn, 1)
            }

            if (!appliedMigrations.contains(2)) {
                migration002(conn)
                recordMigration(conn, 2)
            }
        }
    }

    private fun createMigrationsTable(conn: Connection) {
        conn.createStatement().execute(
            """
            CREATE TABLE IF NOT EXISTS migrations (
                version INTEGER PRIMARY KEY,
                applied_at INTEGER DEFAULT (strftime('%s', 'now'))
            )
            """.trimIndent(),
        )
    }

    private fun getAppliedMigrations(conn: Connection): Set<Int> {
        val migrations = mutableSetOf<Int>()
        conn.createStatement().executeQuery("SELECT version FROM migrations").use { rs ->
            while (rs.next()) {
                migrations.add(rs.getInt("version"))
            }
        }
        return migrations
    }

    private fun recordMigration(
        conn: Connection,
        version: Int,
    ) {
        conn.prepareStatement("INSERT INTO migrations (version) VALUES (?)").use { stmt ->
            stmt.setInt(1, version)
            stmt.executeUpdate()
        }
    }

    private fun migration001(conn: Connection) {
        conn.createStatement().execute(
            """
            CREATE TABLE IF NOT EXISTS upload_queue (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                file_id TEXT NOT NULL UNIQUE,
                chat_id INTEGER NOT NULL,
                user_id INTEGER NOT NULL,
                timestamp INTEGER NOT NULL,
                media_type TEXT NOT NULL,
                status TEXT NOT NULL DEFAULT 'PENDING',
                retry_count INTEGER DEFAULT 0,
                error_message TEXT,
                created_at INTEGER DEFAULT (strftime('%s', 'now') * 1000),
                updated_at INTEGER DEFAULT (strftime('%s', 'now') * 1000)
            )
            """.trimIndent(),
        )

        conn.createStatement().execute(
            """
            CREATE INDEX IF NOT EXISTS idx_status_created ON upload_queue(status, created_at)
            """.trimIndent(),
        )

        conn.createStatement().execute(
            """
            CREATE TABLE IF NOT EXISTS upload_history (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                file_id TEXT NOT NULL,
                chat_id INTEGER NOT NULL,
                user_id INTEGER NOT NULL,
                timestamp INTEGER NOT NULL,
                media_type TEXT NOT NULL,
                status TEXT NOT NULL,
                minio_path TEXT,
                error_message TEXT,
                uploaded_at INTEGER DEFAULT (strftime('%s', 'now') * 1000)
            )
            """.trimIndent(),
        )
    }

    private fun migration002(conn: Connection) {
        // Добавляем колонку original_file_name в upload_queue
        conn.createStatement().execute(
            """
            ALTER TABLE upload_queue ADD COLUMN original_file_name TEXT
            """.trimIndent(),
        )

        // Добавляем колонку original_file_name в upload_history
        conn.createStatement().execute(
            """
            ALTER TABLE upload_history ADD COLUMN original_file_name TEXT
            """.trimIndent(),
        )
    }
}
