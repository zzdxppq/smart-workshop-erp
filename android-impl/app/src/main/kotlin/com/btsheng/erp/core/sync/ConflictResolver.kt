package com.btsheng.erp.core.sync

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/**
 * 冲突解决器（V1.3.7 · Story 1.4 · AC-1.4.4 · P1 修补 ③）
 *
 * 服务端 ts vs 本地 ts（5s 容差），3 选项：覆盖 / 合并 / 取消
 */
object ConflictResolver {

    const val NO_CONFLICT = 0
    const val CONFLICT = 1
    const val THRESHOLD_SEC = 5L

    fun detect(serverTs: LocalDateTime?, localTs: LocalDateTime?): Int {
        if (serverTs == null || localTs == null) return NO_CONFLICT
        val diff = ChronoUnit.SECONDS.between(localTs, serverTs)
        return if (diff > THRESHOLD_SEC) CONFLICT else NO_CONFLICT
    }

    fun resolve(conflictType: Int, userChoice: String?): String {
        if (conflictType == NO_CONFLICT) return "AUTO_OVERWRITE"
        return when (userChoice) {
            "OVERWRITE" -> "USER_OVERWRITE"
            "MERGE" -> "USER_MERGE"
            else -> "USER_CANCEL"
        }
    }
}
