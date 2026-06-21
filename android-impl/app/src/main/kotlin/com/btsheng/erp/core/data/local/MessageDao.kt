package com.btsheng.erp.core.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface MessageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(messages: List<MessageEntity>)

    @Query("SELECT * FROM messages ORDER BY cachedAt DESC LIMIT :limit")
    suspend fun listRecent(limit: Int = 100): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE read = 0 ORDER BY cachedAt DESC LIMIT :limit")
    suspend fun listUnread(limit: Int = 100): List<MessageEntity>

    @Query("UPDATE messages SET read = 1 WHERE id = :id")
    suspend fun markRead(id: Long)

    @Query("DELETE FROM messages WHERE cachedAt < :threshold")
    suspend fun deleteOlderThan(threshold: Long)
}
