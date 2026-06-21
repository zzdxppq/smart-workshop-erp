package com.btsheng.erp.core.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface PendingScanDao {
    @Insert
    suspend fun insert(scan: PendingScanEntity): Long

    @Query("SELECT * FROM pending_scans WHERE synced = 0 ORDER BY localTs ASC")
    suspend fun listPending(): List<PendingScanEntity>

    @Query("SELECT COUNT(*) FROM pending_scans WHERE synced = 0")
    suspend fun countPending(): Int

    @Query("UPDATE pending_scans SET synced = 1 WHERE id = :id")
    suspend fun markSynced(id: Long)

    @Query("DELETE FROM pending_scans WHERE synced = 1")
    suspend fun deleteSynced()
}
