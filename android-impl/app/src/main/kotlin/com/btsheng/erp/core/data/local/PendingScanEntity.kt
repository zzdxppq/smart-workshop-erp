package com.btsheng.erp.core.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** 待同步扫码记录（V1.3.7 · Story 1.4 · AC-1.4.4） */
@Entity(tableName = "pending_scans")
data class PendingScanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val code: String,
    val localTs: Long,
    val deviceId: String,
    val synced: Boolean = false
)
