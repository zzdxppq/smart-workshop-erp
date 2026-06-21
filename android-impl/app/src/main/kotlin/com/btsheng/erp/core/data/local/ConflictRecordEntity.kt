package com.btsheng.erp.core.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** 冲突解决记录（V1.3.7 · Story 1.4 · AC-1.4.4） */
@Entity(tableName = "conflict_records")
data class ConflictRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val code: String,
    val serverTs: Long,
    val localTs: Long,
    val resolution: String,
    val resolvedAt: Long = System.currentTimeMillis()
)
