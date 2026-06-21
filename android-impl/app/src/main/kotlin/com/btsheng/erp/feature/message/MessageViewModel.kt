package com.btsheng.erp.feature.message

/** 消息 ViewModel（V1.3.7 · Story 1.4 · AC-1.4.2） */
class MessageViewModel {
    data class AppMessage(
        val id: Long,
        val type: String,
        val title: String,
        val content: String,
        val routeUrl: String,
        val read: Boolean,
        val timeLabel: String = "",
    )
}
