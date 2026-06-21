package com.btsheng.erp.core.update

import com.btsheng.erp.BuildConfig
import com.btsheng.erp.core.network.SysParamApi

/**
 * APP 新版本检测（PRD 架构 R8 · V1.1 强制升级预留）
 *
 * 读取 sys_param：
 * - app.android-latest-version-code
 * - app.android-latest-version-name
 * - app.android-download-url
 */
object AppVersionChecker {

    data class UpdateInfo(
        val latestVersionCode: Int,
        val latestVersionName: String,
        val downloadUrl: String,
        val forceUpdate: Boolean,
    )

    suspend fun check(api: SysParamApi): UpdateInfo? {
        val latestCode = readInt(api, "app.android-latest-version-code") ?: return null
        if (latestCode <= BuildConfig.VERSION_CODE) return null
        val latestName = readString(api, "app.android-latest-version-name") ?: "新版本"
        val downloadUrl = readString(api, "app.android-download-url").orEmpty()
        val force = readString(api, "app.android-force-update")?.equals("true", ignoreCase = true) == true
        return UpdateInfo(latestCode, latestName, downloadUrl, force)
    }

    fun currentVersionLabel(): String =
        "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}) · PRD ${BuildConfig.PRD_VERSION}"

    private suspend fun readInt(api: SysParamApi, key: String): Int? =
        readString(api, key)?.toIntOrNull()

    private suspend fun readString(api: SysParamApi, key: String): String? = try {
        val resp = api.getByKey(key)
        if (resp.ok) resp.data?.paramValue?.takeIf { it.isNotBlank() } else null
    } catch (_: Exception) {
        null
    }
}
