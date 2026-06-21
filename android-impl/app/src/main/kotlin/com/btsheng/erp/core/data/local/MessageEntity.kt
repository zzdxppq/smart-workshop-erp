package com.btsheng.erp.core.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** 消息缓存（V1.3.7 · Story 1.4 · AC-1.4.2） */
@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: Long,
    val type: String,
    val title: String,
    val content: String,
    val routeUrl: String,
    val read: Boolean,
    val cachedAt: Long = System.currentTimeMillis()
)
