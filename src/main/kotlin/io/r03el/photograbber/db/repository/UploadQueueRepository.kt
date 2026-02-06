package io.r03el.photograbber.db.repository

import io.r03el.photograbber.db.DatabaseConnection
import io.r03el.photograbber.model.QueueItem
import io.r03el.photograbber.model.QueueStatus
import io.r03el.photograbber.model.Type

class UploadQueueRepository(
    private val dbConnection: DatabaseConnection,
) {
    fun enqueue(item: QueueItem): Boolean =
        dbConnection.getConnection().use { conn ->
            conn
                .prepareStatement(
                    """
                    INSERT OR IGNORE INTO upload_queue 
                    (file_id, chat_id, user_id, timestamp, media_type, status, retry_count, error_message, original_file_name)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """.trimIndent(),
                ).use { stmt ->
                    stmt.setString(1, item.fileId)
                    stmt.setLong(2, item.chatId)
                    stmt.setLong(3, item.userId)
                    stmt.setLong(4, item.timestamp)
                    stmt.setString(5, item.mediaType.name)
                    stmt.setString(6, item.status.name)
                    stmt.setInt(7, item.retryCount)
                    stmt.setString(8, item.errorMessage)
                    stmt.setString(9, item.originalFileName)

                    stmt.executeUpdate() > 0
                }
        }

    fun getPendingItems(limit: Int): List<QueueItem> =
        dbConnection.getConnection().use { conn ->
            conn
                .prepareStatement(
                    """
                    SELECT * FROM upload_queue 
                    WHERE status = ? 
                    ORDER BY created_at ASC 
                    LIMIT ?
                    """.trimIndent(),
                ).use { stmt ->
                    stmt.setString(1, QueueStatus.PENDING.name)
                    stmt.setInt(2, limit)

                    stmt.executeQuery().use { rs ->
                        val items = mutableListOf<QueueItem>()
                        while (rs.next()) {
                            items.add(mapResultSetToQueueItem(rs))
                        }
                        items
                    }
                }
        }

    fun markAsProcessing(id: Long): Boolean =
        dbConnection.getConnection().use { conn ->
            conn
                .prepareStatement(
                    """
                    UPDATE upload_queue 
                    SET status = ?, updated_at = (strftime('%s', 'now') * 1000)
                    WHERE id = ? AND status = ?
                    """.trimIndent(),
                ).use { stmt ->
                    stmt.setString(1, QueueStatus.PROCESSING.name)
                    stmt.setLong(2, id)
                    stmt.setString(3, QueueStatus.PENDING.name)
                    stmt.executeUpdate() > 0
                }
        }

    fun markAsCompleted(
        id: Long,
        minioPath: String,
    ) {
        dbConnection.getConnection().use { conn ->
            conn.autoCommit = false
            try {
                val item = getById(conn, id)

                conn
                    .prepareStatement(
                        """
                        INSERT INTO upload_history 
                        (file_id, chat_id, user_id, timestamp, media_type, status, minio_path, original_file_name)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                        """.trimIndent(),
                    ).use { stmt ->
                        stmt.setString(1, item.fileId)
                        stmt.setLong(2, item.chatId)
                        stmt.setLong(3, item.userId)
                        stmt.setLong(4, item.timestamp)
                        stmt.setString(5, item.mediaType.name)
                        stmt.setString(6, QueueStatus.COMPLETED.name)
                        stmt.setString(7, minioPath)
                        stmt.setString(8, item.originalFileName)
                        stmt.executeUpdate()
                    }

                conn.prepareStatement("DELETE FROM upload_queue WHERE id = ?").use { stmt ->
                    stmt.setLong(1, id)
                    stmt.executeUpdate()
                }

                conn.commit()
            } catch (e: Exception) {
                conn.rollback()
                throw e
            } finally {
                conn.autoCommit = true
            }
        }
    }

    fun markAsFailed(
        id: Long,
        errorMessage: String,
        @Suppress("UNUSED_PARAMETER") maxRetries: Int,
    ) {
        dbConnection.getConnection().use { conn ->
            conn
                .prepareStatement(
                    """
                    UPDATE upload_queue 
                    SET status = ?, 
                        error_message = ?, 
                        retry_count = retry_count + 1,
                        updated_at = (strftime('%s', 'now') * 1000)
                    WHERE id = ?
                    """.trimIndent(),
                ).use { stmt ->
                    stmt.setString(1, QueueStatus.FAILED.name)
                    stmt.setString(2, errorMessage)
                    stmt.setLong(3, id)
                    stmt.executeUpdate()
                }
        }
    }

    fun resetFailedToPending(maxRetries: Int) {
        dbConnection.getConnection().use { conn ->
            conn
                .prepareStatement(
                    """
                    UPDATE upload_queue 
                    SET status = ?, error_message = NULL
                    WHERE status = ? AND retry_count < ?
                    """.trimIndent(),
                ).use { stmt ->
                    stmt.setString(1, QueueStatus.PENDING.name)
                    stmt.setString(2, QueueStatus.FAILED.name)
                    stmt.setInt(3, maxRetries)
                    stmt.executeUpdate()
                }
        }
    }

    fun getQueueStats(): Map<String, Int> =
        dbConnection.getConnection().use { conn ->
            conn
                .createStatement()
                .executeQuery(
                    """
                    SELECT status, COUNT(*) as count FROM upload_queue GROUP BY status
                    """.trimIndent(),
                ).use { rs ->
                    val stats = mutableMapOf<String, Int>()
                    while (rs.next()) {
                        stats[rs.getString("status")] = rs.getInt("count")
                    }
                    stats
                }
        }

    fun getCompletedCount(): Int =
        dbConnection.getConnection().use { conn ->
            conn
                .createStatement()
                .executeQuery(
                    """
                    SELECT COUNT(*) as count FROM upload_history
                    """.trimIndent(),
                ).use { rs ->
                    if (rs.next()) rs.getInt("count") else 0
                }
        }

    fun getLastUploadTime(): Long? =
        dbConnection.getConnection().use { conn ->
            conn
                .createStatement()
                .executeQuery(
                    """
                    SELECT MAX(uploaded_at) as last_upload FROM upload_history
                    """.trimIndent(),
                ).use { rs ->
                    if (rs.next()) {
                        rs.getLong("last_upload").takeIf { it > 0 }
                    } else {
                        null
                    }
                }
        }

    fun getAverageProcessingTime(): Double? =
        dbConnection.getConnection().use { conn ->
            conn
                .createStatement()
                .executeQuery(
                    """
                    SELECT AVG(uploaded_at - timestamp) as avg_time FROM upload_history
                    """.trimIndent(),
                ).use { rs ->
                    if (rs.next()) {
                        rs.getDouble("avg_time").takeIf { !rs.wasNull() }
                    } else {
                        null
                    }
                }
        }

    fun getMediaTypeStats(): Map<String, Int> =
        dbConnection.getConnection().use { conn ->
            conn
                .createStatement()
                .executeQuery(
                    """
                    SELECT media_type, COUNT(*) as count FROM upload_history GROUP BY media_type
                    """.trimIndent(),
                ).use { rs ->
                    val stats = mutableMapOf<String, Int>()
                    while (rs.next()) {
                        stats[rs.getString("media_type")] = rs.getInt("count")
                    }
                    stats
                }
        }

    private fun getById(
        conn: java.sql.Connection,
        id: Long,
    ): QueueItem =
        conn.prepareStatement("SELECT * FROM upload_queue WHERE id = ?").use { stmt ->
            stmt.setLong(1, id)
            stmt.executeQuery().use { rs ->
                if (rs.next()) {
                    mapResultSetToQueueItem(rs)
                } else {
                    throw IllegalArgumentException("Queue item with id $id not found")
                }
            }
        }

    private fun mapResultSetToQueueItem(rs: java.sql.ResultSet): QueueItem =
        QueueItem(
            id = rs.getLong("id"),
            fileId = rs.getString("file_id"),
            chatId = rs.getLong("chat_id"),
            userId = rs.getLong("user_id"),
            timestamp = rs.getLong("timestamp"),
            mediaType = Type.valueOf(rs.getString("media_type")),
            status = QueueStatus.valueOf(rs.getString("status")),
            retryCount = rs.getInt("retry_count"),
            errorMessage = rs.getString("error_message"),
            originalFileName = rs.getString("original_file_name"),
            createdAt = rs.getLong("created_at"),
            updatedAt = rs.getLong("updated_at"),
        )
}
