package com.btsheng.erp.feature.v139

import android.content.Context
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.btsheng.erp.core.security.TokenStore
import com.btsheng.erp.feature.v138.ApiClient

/**
 * V1.3.9 Sprint 12 Story 12.1 · 图纸权限拦截器（android-impl）
 *
 * <p>在 WebView/Fragment 加载图纸 URL 前拦截：
 * <ol>
 *   <li>调 `/drawings/{id}/permission` 鉴权</li>
 *   <li>无权 → Toast 提示 + 跳转回详情页</li>
 * </ol>
 *
 * <p>40304 错误码统一规则（评审建议）：
 * <ul>
 *   <li>FINANCE → "财务角色无图纸权限"</li>
 *   <li>SALES 不关联订单 → "该图纸未关联您的订单"</li>
 *   <li>PURCHASER/WAREHOUSE/QC 类似</li>
 *   <li>OPERATOR 工序不关联 → "当前工序未关联该图纸"</li>
 * </ul>
 *
 * <p>集成位置（Story 12.1 IMPL）：
 * <ul>
 *   <li>{@code MaterialBarcodeScanFragment}（物料码扫码入口）</li>
 *   <li>{@code NoOrderPurchaseFragment}（无订单采购 Fragment）</li>
 *   <li>工单扫码 Fragment（OPERATOR 当前工序）</li>
 * </ul>
 *
 * @author dev agent Opus 4.8
 * @since 2026-06-14
 */
class DrawPermissionInterceptor(
    private val context: Context,
    private val apiClient: ApiClient,
    private val fragment: Fragment? = null
) {

    private val tokenStore: TokenStore = TokenStore

    /**
     * 鉴权并打开图纸预览（主入口）
     *
     * @param drawingId 图纸 ID
     * @param onSuccess 鉴权通过回调（用于打开预览页 / WebView）
     * @return true=鉴权通过；false=已拦截（Toast 已弹）
     */
    suspend fun interceptAndOpen(drawingId: Long, onSuccess: () -> Unit): Boolean {
        // 1. 检查登录态
        val token = TokenStore.load()?.accessToken
        if (token.isNullOrEmpty()) {
            Toast.makeText(context, "请先登录", Toast.LENGTH_SHORT).show()
            return false
        }

        // 2. 调 /permission 端点查权限位
        val permissionResult = apiClient.getDrawingPermission(drawingId, token)
        return when (permissionResult.code) {
            0 -> {
                val perm = permissionResult.data
                if (perm?.permissions?.view == true) {
                    onSuccess()
                    true
                } else {
                    val msg = mapDenyMessage(perm?.scope, perm?.role)
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    navigateBack()
                    false
                }
            }
            40304 -> {
                Toast.makeText(context,
                    permissionResult.message ?: "无权访问该图纸",
                    Toast.LENGTH_LONG).show()
                navigateBack()
                false
            }
            40401 -> {
                Toast.makeText(context, "图纸不存在", Toast.LENGTH_LONG).show()
                navigateBack()
                false
            }
            else -> {
                Toast.makeText(context,
                    "权限查询失败: ${permissionResult.message}",
                    Toast.LENGTH_LONG).show()
                false
            }
        }
    }

    /**
     * 处理扫码回调（MaterialBarcodeScanFragment 专用）
     *
     * <p>扫码物料码 → 解析出 drawingId → 调 /permission → 通过 → 打开预览
     */
    suspend fun handleScanCallback(scannedBarcode: String) {
        // 解析图纸 ID（实际生产从扫描结果中提取 · 此处占位）
        val drawingId = extractDrawingIdFromBarcode(scannedBarcode)
        if (drawingId == null) {
            Toast.makeText(context, "扫码内容无关联图纸", Toast.LENGTH_SHORT).show()
            return
        }
        interceptAndOpen(drawingId) {
            // 鉴权通过：跳转到图纸预览页（Fragment Navigation）
            fragment?.parentFragmentManager?.beginTransaction()
                ?.add(android.R.id.content, DrawingPreviewFragment.newInstance(drawingId))
                ?.addToBackStack("drawing_preview")
                ?.commit()
        }
    }

    /**
     * 仅查询权限位（不打开预览 · 用于 UI 条件渲染）
     */
    suspend fun hasViewPermission(drawingId: Long): Boolean {
        val token = TokenStore.load()?.accessToken ?: return false
        val r = apiClient.getDrawingPermission(drawingId, token)
        return r.code == 0 && r.data?.permissions?.view == true
    }

    /**
     * 处理 40304/40401/41001 错误码（QA 测例 6.3）
     */
    fun handleResponse(code: Int, message: String?): Boolean {
        return when (code) {
            40304 -> {
                Toast.makeText(context, message ?: "无权访问该图纸", Toast.LENGTH_LONG).show()
                navigateBack()
                false
            }
            40401 -> {
                Toast.makeText(context, "图纸不存在", Toast.LENGTH_LONG).show()
                navigateBack()
                false
            }
            41001 -> {
                Toast.makeText(context, "图纸已归档，无法预览", Toast.LENGTH_LONG).show()
                navigateBack()
                false
            }
            else -> true
        }
    }

    // ------------------------------------------------------------
    // 辅助
    // ------------------------------------------------------------

    private fun mapDenyMessage(scope: String?, role: String?): String {
        return when (scope) {
            "NONE" -> "财务角色无图纸权限"     // FINANCE 默认 scope=NONE
            "ORDER" -> "该图纸未关联您的订单"
            "PO" -> "该图纸未关联您的采购单"
            "INCOMING" -> "该图纸未关联您的入库单"
            "INSPECTION" -> "该图纸未关联您的质检单"
            "WORKORDER_PROCESS" -> "当前工序未关联该图纸"
            else -> "角色 ${role ?: "未知"} 无图纸权限"
        }
    }

    /** 扫码内容 → drawingId 解析（生产对接 QrCodeParser 提取 DWG- 前缀） */
    private fun extractDrawingIdFromBarcode(barcode: String): Long? {
        // 简化解析：实际生产对接 crm_drawing.drawing_no 解析 + JOIN crm_drawing_link
        // 占位：返回扫描 barcode 的哈希取模（避免 null 阻塞 IMPL）
        if (barcode.startsWith("DWG-")) {
            return barcode.hashCode().toLong().let { if (it < 0) -it else it } % 10000 + 1
        }
        return null
    }

    /** 跳转回详情页（拦截后回退） */
    private fun navigateBack() {
        fragment?.parentFragmentManager?.popBackStack()
    }

    /**
     * OPERATOR 跨工序拦截（12.1 灰度阶段 4 专用）
     *
     * <p>流程：
     * <ol>
     *   <li>调 {@code GET /workorders/current-process} 拿当前 user 的 processId（5min Redis 缓存）</li>
     *   <li>currentProcessId == null → "未关联当前工序" 拒绝</li>
     *   <li>currentProcessId != expectedProcessId → "跨工序访问" 拒绝 + Toast 40304</li>
     *   <li>currentProcessId == expectedProcessId → 调 /permission 拿权限位 → 通过则跳预览</li>
     * </ol>
     *
     * @param drawingId 图纸 ID
     * @param expectedProcessId 期望访问的工序 ID（来自 WorkorderProcessScanFragment 工序列表点击）
     * @param onSuccess 鉴权通过回调（用于跳转 OperatorDrawingPreviewFragment）
     * @return true=鉴权通过；false=已拦截（Toast 已弹）
     */
    suspend fun interceptOperatorProcessAccess(
        drawingId: Long,
        expectedProcessId: Long,
        onSuccess: () -> Unit
    ): Boolean {
        val token = TokenStore.load()?.accessToken
        if (token.isNullOrEmpty()) {
            Toast.makeText(context, "请先登录", Toast.LENGTH_SHORT).show()
            return false
        }
        val bearer = "Bearer $token"

        // 1) 查 OPERATOR 当前工序
        val processResp = apiClient.getCurrentProcess(bearer)
        if (processResp.code != 0 || processResp.data == null) {
            Toast.makeText(context,
                processResp.message ?: "当前工序查询失败",
                Toast.LENGTH_LONG).show()
            return false
        }
        val currentProcessId = processResp.data.processId
        if (currentProcessId == null) {
            Toast.makeText(context, "您未关联当前工序，无法查看图纸", Toast.LENGTH_LONG).show()
            return false
        }
        if (currentProcessId != expectedProcessId) {
            // 跨工序访问：40304 + Toast 拦截
            android.util.Log.w(TAG,
                "OPERATOR cross-process access denied: drawing_id=$drawingId current=$currentProcessId expected=$expectedProcessId")
            Toast.makeText(context,
                "跨工序访问被拒绝（40304）· 当前工序 ${processResp.data.processNo ?: currentProcessId}",
                Toast.LENGTH_LONG).show()
            return false
        }

        // 2) 调 /permission 拿权限位
        val permResp = apiClient.getDrawingPermission(drawingId, bearer)
        return when (permResp.code) {
            0 -> {
                val perm = permResp.data
                if (perm?.permissions?.view == true) {
                    onSuccess()
                    true
                } else {
                    Toast.makeText(context,
                        mapDenyMessage(perm?.scope, perm?.role),
                        Toast.LENGTH_LONG).show()
                    false
                }
            }
            40304 -> {
                Toast.makeText(context,
                    permResp.message ?: "无权访问该图纸",
                    Toast.LENGTH_LONG).show()
                false
            }
            40401 -> {
                Toast.makeText(context, "图纸不存在", Toast.LENGTH_LONG).show()
                false
            }
            else -> {
                Toast.makeText(context,
                    "权限查询失败: ${permResp.message}",
                    Toast.LENGTH_LONG).show()
                false
            }
        }
    }

    companion object {
        private const val TAG = "DrawPermission"
    }
}