package com.btsheng.erp.core.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [MessageEntity::class, PendingScanEntity::class, ConflictRecordEntity::class],
    version = 1,
    exportSchema = false
)
abstract class ErpDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
    abstract fun pendingScanDao(): PendingScanDao
    abstract fun conflictRecordDao(): ConflictRecordDao
}
