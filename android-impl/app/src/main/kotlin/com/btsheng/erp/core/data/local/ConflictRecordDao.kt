package com.btsheng.erp.core.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ConflictRecordDao {
    @Insert
    suspend fun insert(record: ConflictRecordEntity): Long

    @Query("SELECT * FROM conflict_records ORDER BY resolvedAt DESC LIMIT :limit")
    suspend fun listRecent(limit: Int = 100): List<ConflictRecordEntity>
}
