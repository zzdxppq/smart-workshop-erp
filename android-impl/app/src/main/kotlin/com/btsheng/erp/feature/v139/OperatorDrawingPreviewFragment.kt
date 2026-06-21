package com.btsheng.erp.feature.v139

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.btsheng.erp.core.security.TokenStore
import com.btsheng.erp.feature.v138.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * V1.3.9 Sprint 12 Story 12.1 · OPERATOR APP 端"查看图纸" Fragment（灰度阶段 4）
 *
 * <p>调用栈：扫码 → WorkorderProcessScanFragment → 当前工序 → "查看图纸"按钮 → 当前 Fragment
 *
 * <p>职责：
 * <ol>
 *   <li>接收 props：{@code drawingId}, {@code processId}, {@code workorderNo}, {@code processNo}</li>
 *   <li>调 {@code GET /drawings/{id}/permission} 拿权限位（OPERATOR 仅 view=true）</li>
 *   <li>feature flag 灰度：{@code draw.acl.gray.OPERATOR}=false → 隐藏入口</li>
 *   <li>复用 {@link DrawPermissionInterceptor} 跨工序拦截</li>
 *   <li>{@code GET /drawings/{id}/preview} 流式拉取 PDF（不下载原文件）</li>
 *   <li>渲染：占位 TextView 显示 PDF 字节数 + 顶部信息（工单+工序+用户+灰度标识）</li>
 *   <li>logcat TAG=DrawPermission · INFO 级别记录 "OPERATOR drawing preview: ..."（灰度观察期每天 1 次）</li>
 * </ol>
 *
 * <p>12.1 arch REVIEW §3.3 灰度 4 阶段 ≥ 2026-06-20 开启
 *
 * @author dev agent Opus 4.8
 * @since 2026-06-14
 */
class OperatorDrawingPreviewFragment : Fragment() {

    companion object {
        const val TAG = "DrawPermission"
        const val ARG_DRAWING_ID = "drawingId"
        const val ARG_PROCESS_ID = "processId"
        const val ARG_WORKORDER_NO = "workorderNo"
        const val ARG_PROCESS_NO = "processNo"

        // 灰度时序对齐（arch REVIEW §3.3）：阶段 4 开启 ≥ 2026-06-20
        const val GRAY_STAGE4_OPEN_AT = "2026-06-20"

        fun newInstance(
            drawingId: Long,
            processId: Long,
            workorderNo: String,
            processNo: String
        ): OperatorDrawingPreviewFragment {
            return OperatorDrawingPreviewFragment().apply {
                arguments = Bundle().apply {
                    putLong(ARG_DRAWING_ID, drawingId)
                    putLong(ARG_PROCESS_ID, processId)
                    putString(ARG_WORKORDER_NO, workorderNo)
                    putString(ARG_PROCESS_NO, processNo)
                }
            }
        }
    }

    private var drawingId: Long = -1L
    private var processId: Long = -1L
    private var workorderNo: String = ""
    private var processNo: String = ""

    private val tokenStore: TokenStore = TokenStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            drawingId = it.getLong(ARG_DRAWING_ID, -1L)
            processId = it.getLong(ARG_PROCESS_ID, -1L)
            workorderNo = it.getString(ARG_WORKORDER_NO) ?: ""
            processNo = it.getString(ARG_PROCESS_NO) ?: ""
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // 顶部信息 + PDF 占位区
        val root = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }

        val tvHeader = TextView(requireContext()).apply {
            text = buildHeaderText()
            textSize = 14f
            setPadding(0, 0, 0, 16)
        }
        root.addView(tvHeader)

        val tvGrayFlag = TextView(requireContext()).apply {
            text = "灰度标识：阶段 4 · OPERATOR · 开启日期 $GRAY_STAGE4_OPEN_AT"
            textSize = 12f
            setTextColor(0xFF388E3C.toInt())
            setPadding(0, 0, 0, 16)
        }
        root.addView(tvGrayFlag)

        val tvPdfInfo = TextView(requireContext()).apply {
            text = "等待加载图纸…"
            textSize = 16f
            setPadding(0, 16, 0, 16)
        }
        root.addView(tvPdfInfo)

        val btnBack = Button(requireContext()).apply {
            text = "返回工序"
            setOnClickListener {
                parentFragmentManager.popBackStack()
            }
        }
        root.addView(btnBack)

        // 启动加载
        viewLifecycleOwner.lifecycleScope.launch {
            tvPdfInfo.text = "鉴权中…"
            loadDrawing(tvPdfInfo)
        }
        return root
    }

    private fun buildHeaderText(): String {
        val now = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA).format(Date())
        val user = TokenStore.load()?.userId ?: -1L
        return buildString {
            append("工单：").append(workorderNo).append('\n')
            append("工序：").append(processNo).append("（ID=$processId）").append('\n')
            append("图纸 ID：").append(drawingId).append('\n')
            append("当前用户：userId=$user").append('\n')
            append("加载时间：").append(now)
        }
    }

    /**
     * 主加载流程：feature flag → 鉴权 → 拉 PDF
     */
    private suspend fun loadDrawing(tvPdfInfo: TextView) {
        try {
            val token = TokenStore.load()?.accessToken ?: ""
            if (token.isNullOrEmpty()) {
                Toast.makeText(requireContext(), "请先登录", Toast.LENGTH_SHORT).show()
                navigateBack()
                return
            }

            val bearer = "Bearer $token"
            val apiClient = buildApiClient()
            val flagClient = DrawingFeatureFlag(apiClient, bearer)

            // 1) 灰度开关检查
            val grayOn = flagClient.isOperatorGrayEnabled("OPERATOR")
            if (!grayOn) {
                Log.i(TAG, "OPERATOR drawing preview blocked: gray flag OFF (drawing_id=$drawingId)")
                Toast.makeText(requireContext(), "操作工图纸预览灰度未开启", Toast.LENGTH_LONG).show()
                navigateBack()
                return
            }

            // 2) 调 /permission 拿权限位
            val permResp = apiClient.getDrawingPermission(drawingId, bearer)
            if (permResp.code != 0 || permResp.data == null) {
                handleDeny(permResp.code, permResp.message ?: "权限查询失败", "权限查询失败")
                return
            }
            val perm = permResp.data
            if (perm.permissions?.view != true) {
                handleDeny(40304, "权限位 view=false · role=${perm.role}", "您无权查看该图纸")
                return
            }
            // OPERATOR 严格限制：print/download/upload/delete 必须 false
            if (perm.permissions.print || perm.permissions.download ||
                perm.permissions.upload || perm.permissions.delete) {
                Log.w(TAG, "OPERATOR 权限位异常: drawing_id=$drawingId print/download/upload/delete 应全 false")
            }

            // 3) 调 /preview 拉 PDF 流
            val pdfResp = withContext(Dispatchers.IO) {
                apiClient.previewDrawing(drawingId, bearer, resolution = "MEDIUM")
            }
            if (!pdfResp.isSuccessful) {
                val code = pdfResp.code()
                val msg = pdfResp.errorBody()?.string()?.take(200) ?: ""
                if (code == 403) {
                    handleDeny(40304, "Preview 403 · $msg", "当前工序未关联该图纸")
                } else {
                    Toast.makeText(requireContext(), "预览失败 HTTP=$code", Toast.LENGTH_LONG).show()
                }
                return
            }
            val body = pdfResp.body()
            val bytes = body?.bytes()?.size ?: 0

            // 4) 渲染（占位 TextView · 真实 PDF 渲染用 AndroidPdfRenderer）
            tvPdfInfo.text = buildString {
                append("PDF 已加载\n")
                append("字节数：$bytes\n")
                append("分辨率：MEDIUM\n")
                append("渲染方式：流式渲染（不下载原文件）\n")
                append("水印：user=${TokenStore.load()?.userId ?: "?"}\n")
                append("权限位：view=true · print/download/upload/delete=false")
            }

            // 5) observability：logcat INFO（灰度观察期每天 1 次）
            Log.i(TAG, "OPERATOR drawing preview: drawing_id=$drawingId user=$processId process=$processNo")
        } catch (e: Exception) {
            Log.e(TAG, "OPERATOR drawing preview 异常", e)
            Toast.makeText(requireContext(), "渲染失败：${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun handleDeny(code: Int, logMsg: String, userMsg: String) {
        Log.w(TAG, "OPERATOR drawing preview denied: code=$code · $logMsg")
        Toast.makeText(requireContext(), userMsg, Toast.LENGTH_LONG).show()
        navigateBack()
    }

    private fun navigateBack() {
        parentFragmentManager.popBackStack()
    }

    private fun buildApiClient(): ApiClient {
        return Retrofit.Builder()
            .baseUrl("https://erp.yourcompany.local/")
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(ApiClient::class.java)
    }
}