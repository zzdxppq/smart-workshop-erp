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
import com.btsheng.erp.BuildConfig
import com.btsheng.erp.core.security.TokenStore
import com.btsheng.erp.feature.v138.ApiClient
import com.btsheng.erp.feature.v138.CurrentProcessApiResponse
import com.btsheng.erp.feature.v138.WorkorderProcessItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

/**
 * V1.3.9 Sprint 12 Story 12.1 · 工单扫码 Fragment（OPERATOR 灰度阶段 4 入口）
 *
 * <p>调用栈：
 * <ol>
 *   <li>OPERATOR 扫码工单码 → 跳转本 Fragment</li>
 *   <li>{@code GET /workorders/current-process} 查当前工序</li>
 *   <li>{@code GET /workorders/{id}/processes} 渲染真实工序列表</li>
 *   <li>点击「查看图纸」→ {@link DrawPermissionInterceptor#interceptOperatorProcessAccess}</li>
 * </ol>
 */
class WorkorderProcessScanFragment : Fragment() {

    companion object {
        const val TAG = "DrawPermission"
        const val ARG_WORKORDER_NO = "workorderNo"

        fun newInstance(workorderNo: String): WorkorderProcessScanFragment {
            return WorkorderProcessScanFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_WORKORDER_NO, workorderNo)
                }
            }
        }
    }

    private var workorderNo: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        workorderNo = arguments?.getString(ARG_WORKORDER_NO) ?: ""
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val root = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }
        val tvTitle = TextView(requireContext()).apply {
            text = "工单扫码（V1.3.9 · 12.1 OPERATOR 灰度阶段 4）"
            textSize = 18f
            setPadding(0, 0, 0, 8)
        }
        root.addView(tvTitle)
        val tvHint = TextView(requireContext()).apply {
            text = buildString {
                append("工单号：").append(workorderNo).append('\n')
                append("扫描后查看工序列表 · 当前工序可查看图纸")
            }
            textSize = 14f
            setPadding(0, 0, 0, 16)
        }
        root.addView(tvHint)
        val tvProcessList = TextView(requireContext()).apply {
            text = "正在加载工序列表…"
            textSize = 14f
        }
        root.addView(tvProcessList)
        val btnContainer = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 16, 0, 0)
        }
        root.addView(btnContainer)

        viewLifecycleOwner.lifecycleScope.launch {
            loadProcessList(tvProcessList, btnContainer)
        }
        return root
    }

    private suspend fun loadProcessList(tvProcessList: TextView, btnContainer: LinearLayout) {
        try {
            val token = TokenStore.load()?.accessToken ?: ""
            if (token.isEmpty()) {
                Toast.makeText(requireContext(), "请先登录", Toast.LENGTH_SHORT).show()
                return
            }
            val bearer = "Bearer $token"
            val apiClient = buildApiClient()

            val flagClient = DrawingFeatureFlag(apiClient, bearer)
            val grayOn = flagClient.isOperatorGrayEnabled("OPERATOR")
            if (!grayOn) {
                tvProcessList.text = "操作工图纸预览灰度未开启（draw.acl.gray.OPERATOR=false）"
                Log.i(TAG, "WorkorderProcessScan blocked: gray flag OFF")
                return
            }

            val resp: CurrentProcessApiResponse = withContext(Dispatchers.IO) {
                apiClient.getCurrentProcess(bearer)
            }
            if (resp.code != 0 || resp.data == null) {
                tvProcessList.text = "当前工序查询失败：${resp.message ?: "未知"}"
                return
            }
            val data = resp.data
            val currentProcessId = data.processId
            if (currentProcessId == null) {
                tvProcessList.text = "您未关联当前工序 · 无法查看图纸"
                return
            }
            val workorderId = data.workorderId
            if (workorderId == null) {
                tvProcessList.text = "当前工序未关联工单 ID"
                return
            }

            val processResp = withContext(Dispatchers.IO) {
                apiClient.listWorkorderProcesses(workorderId, bearer)
            }
            if (processResp.code != 0 || processResp.data.isNullOrEmpty()) {
                tvProcessList.text = "工序列表加载失败：${processResp.message ?: "无数据"}"
                return
            }

            val processList = processResp.data
            val drawingId = data.drawingId
            tvProcessList.text = buildString {
                append("工序列表（").append(data.workorderNo ?: workorderNo).append("）：\n")
                processList.forEach { item ->
                    val marker = if (item.id == currentProcessId) " ← 当前" else ""
                    append("· ")
                        .append(item.processCode ?: "P??")
                        .append("（ID=").append(item.id).append("）")
                        .append(" · ")
                        .append(item.processName ?: item.status ?: "")
                        .append(marker)
                        .append('\n')
                }
                if (data.cached) {
                    append("（来自 Redis 5min 缓存）")
                }
            }

            btnContainer.removeAllViews()
            processList.forEach { item ->
                val processId = item.id ?: return@forEach
                val processNo = item.processCode ?: "P??"
                val btn = Button(requireContext()).apply {
                    text = if (processId == currentProcessId) {
                        "查看图纸（当前工序）"
                    } else {
                        "查看图纸（$processNo · 跨工序将被拦截）"
                    }
                    setOnClickListener {
                        viewLifecycleOwner.lifecycleScope.launch {
                            onViewDrawingClicked(apiClient, bearer, drawingId, processId, processNo)
                        }
                    }
                }
                btnContainer.addView(btn)
            }
        } catch (e: Exception) {
            Log.e(TAG, "loadProcessList 异常", e)
            tvProcessList.text = "加载失败：${e.message}"
        }
    }

    private suspend fun onViewDrawingClicked(
        apiClient: ApiClient,
        bearer: String,
        drawingId: Long?,
        processId: Long,
        processNo: String
    ) {
        try {
            if (drawingId == null) {
                Toast.makeText(requireContext(), "当前工单未关联图纸", Toast.LENGTH_LONG).show()
                return
            }
            Log.i(TAG, "OPERATOR 点击查看图纸: drawing_id=$drawingId process_id=$processId process_no=$processNo")

            val interceptor = DrawPermissionInterceptor(requireContext(), apiClient, this)
            val passed = interceptor.interceptOperatorProcessAccess(
                drawingId = drawingId,
                expectedProcessId = processId
            ) {
                parentFragmentManager.beginTransaction()
                    .replace(android.R.id.content, OperatorDrawingPreviewFragment.newInstance(
                        drawingId = drawingId,
                        processId = processId,
                        workorderNo = workorderNo,
                        processNo = processNo
                    ))
                    .addToBackStack("operator_drawing_preview")
                    .commit()
            }
            if (!passed) {
                Log.w(TAG, "OPERATOR 跨工序拦截: drawing_id=$drawingId process_id=$processId")
            }
        } catch (e: Exception) {
            Log.e(TAG, "onViewDrawingClicked 异常", e)
            Toast.makeText(requireContext(), "跳转失败：${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun buildApiClient(): ApiClient {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(ApiClient::class.java)
    }
}
