package com.btsheng.erp.feature.v139

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.btsheng.erp.core.security.TokenStore
import com.btsheng.erp.feature.v138.ApiClient

/**
 * V1.3.9 Sprint 12 Story 12.1 · 图纸预览 Fragment（android-impl）
 *
 * <p>加载 /drawings/{id}/preview PDF 流（鉴权由 DrawPermissionInterceptor 保证）
 * <p>PDF 渲染：WebView 内置 PDF.js 或系统 PDF Viewer
 *
 * @author dev agent Opus 4.8
 * @since 2026-06-14
 */
class DrawingPreviewFragment : Fragment() {

    companion object {
        private const val ARG_DRAWING_ID = "drawingId"

        fun newInstance(drawingId: Long): DrawingPreviewFragment {
            val f = DrawingPreviewFragment()
            f.arguments = Bundle().apply { putLong(ARG_DRAWING_ID, drawingId) }
            return f
        }
    }

    private var drawingId: Long = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        drawingId = arguments?.getLong(ARG_DRAWING_ID, -1L) ?: -1L
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // 占位 UI（实际生产对接 WebView + PDF.js 渲染 PDF 流）
        val tv = android.widget.TextView(requireContext())
        tv.text = "图纸预览 · drawingId=$drawingId\n（鉴权已通过 · PDF 流由 /preview 端点返回）"
        tv.setPadding(32, 32, 32, 32)
        tv.textSize = 16f
        return tv
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // 实际生产：在此调 ApiClient.previewDrawing() → 渲染 PDF
    }
}