package com.btsheng.erp.feature.v139

import android.util.Log
import com.btsheng.erp.feature.v138.ApiClient

/**
 * V1.3.9 Sprint 12 Story 12.1 · OPERATOR 灰度 feature flag 客户端
 *
 * <p>读取后端 sys_dict `DRAWING_ACL_FEATURE_FLAG` 表，灰度开关如下：
 * <ul>
 *   <li>{@code draw.acl.gray.OPERATOR} = true → 开启 OPERATOR 图纸预览入口</li>
 *   <li>{@code draw.acl.gray.OPERATOR} = false → OPERATOR 完全看不到入口（默认）</li>
 * </ul>
 *
 * <p>灰度开关缓存策略：
 * <ul>
 *   <li>JVM 进程内 5 分钟缓存（与 backend @Cacheable TTL 对齐）</li>
 *   <li>缓存在 process restart 时失效</li>
 *   <li>实测 /dicts 端点拉取 DRAWING_ACL_FEATURE_FLAG</li>
 * </ul>
 *
 * <p>12.1 arch REVIEW §3.3 灰度 4 阶段：
 * 阶段 1 admin/ENGINEER → 阶段 2 SALES → 阶段 3 PUR/WH/QC → 阶段 4 OPERATOR (≥ 2026-06-20)
 *
 * @author dev agent Opus 4.8
 * @since 2026-06-14
 */
class DrawingFeatureFlag(
    private val apiClient: ApiClient,
    private val bearerToken: String
) {

    companion object {
        const val TAG = "DrawPermission"
        const val TYPE = "DRAWING_ACL_FEATURE_FLAG"
        const val KEY_OPERATOR = "draw.acl.gray.OPERATOR"
        const val KEY_ENGINEER = "draw.acl.gray.ENGINEER"
        const val KEY_SALES = "draw.acl.gray.SALES"
        const val KEY_PURCHASER = "draw.acl.gray.PURCHASER"
        const val KEY_WAREHOUSE = "draw.acl.gray.WAREHOUSE"
        const val KEY_QC = "draw.acl.gray.QC"

        // 进程内 5 分钟缓存（与 backend @Cacheable TTL 对齐）
        private const val CACHE_TTL_MS = 5L * 60L * 1000L

        @Volatile private var cache: Map<String, Boolean> = emptyMap()
        @Volatile private var cacheAtMs: Long = 0L

        /**
         * 解析 sys_dict value："true"/"1"/"TRUE" → true；其他 → false
         */
        fun parseFlagValue(raw: String?): Boolean {
            if (raw == null) return false
            val v = raw.trim().lowercase()
            return v == "true" || v == "1"
        }
    }

    /**
     * 读取全量 DRAWING_ACL_FEATURE_FLAG 缓存
     */
    suspend fun loadFlags(): Map<String, Boolean> {
        val now = System.currentTimeMillis()
        if (cache.isNotEmpty() && (now - cacheAtMs) < CACHE_TTL_MS) {
            return cache
        }
        return try {
            val resp = apiClient.listDrawingFeatureFlags(TYPE, bearerToken)
            if (resp.code == 0 && resp.data != null) {
                val parsed = resp.data
                    .filter { it.dictType == TYPE }
                    .associate { it.dictCode.orEmpty() to parseFlagValue(it.dictLabel) }
                cache = parsed
                cacheAtMs = now
                Log.i(TAG, "Loaded DRAWING_ACL_FEATURE_FLAG: $parsed")
                parsed
            } else {
                // 拉取失败保守返空 → 全部 false（保持 V1.3.7 全员可见行为）
                Log.w(TAG, "loadFlags failed: code=${resp.code} message=${resp.message}")
                emptyMap()
            }
        } catch (e: Exception) {
            Log.w(TAG, "loadFlags exception", e)
            emptyMap()
        }
    }

    /**
     * 单角色 feature flag 查询（带缓存）
     *
     * @param roleCode OPERATOR / ENGINEER / SALES / ...
     * @return true=灰度开启；false=灰度关闭或未知
     */
    suspend fun isOperatorGrayEnabled(roleCode: String = "OPERATOR"): Boolean {
        val flags = loadFlags()
        val key = when (roleCode) {
            "OPERATOR" -> KEY_OPERATOR
            "ENGINEER" -> KEY_ENGINEER
            "SALES" -> KEY_SALES
            "PURCHASER" -> KEY_PURCHASER
            "WAREHOUSE" -> KEY_WAREHOUSE
            "QC" -> KEY_QC
            else -> return false
        }
        return flags[key] ?: false
    }

    /**
     * 清空缓存（紧急回退用：admin 改 sys_dict 后立即生效）
     */
    fun invalidate() {
        cache = emptyMap()
        cacheAtMs = 0L
        Log.i(TAG, "DrawingFeatureFlag cache invalidated")
    }
}