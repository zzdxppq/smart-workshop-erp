package com.btsheng.erp.feature.v139

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.btsheng.erp.feature.v138.ApiClient
import com.btsheng.erp.feature.v138.LabelPreviewRequest
import com.btsheng.erp.feature.v138.LabelPreviewData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

/**
 * V1.3.9 Sprint 12 Story 12.3 · 标签预览 Fragment (android-impl · AC-12.3.4 / TC-12.3.4.2)
 *
 * <p>扫码后跳转至此 Fragment · 显示 GD-/LZ-/SB-/WW-/WL- 标签 PNG
 * <p>调后端 /label-templates/preview 拿 base64 · 解析后 BitmapFactory 显示
 *
 * @author dev agent Opus 4.8 · 2026-06-14
 */
class LabelPreviewFragment : Fragment() {

    private var _imageView: ImageView? = null

    private var labelType: String = "GD"
    private var qrContent: String = ""
    private var lines: List<String> = emptyList()
    private var factoryName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            labelType = it.getString(ARG_TYPE) ?: "GD"
            qrContent = it.getString(ARG_QR_CONTENT) ?: ""
            @Suppress("UNCHECKED_CAST")
            lines = (it.getSerializable(ARG_LINES) as? ArrayList<String>) ?: emptyList()
            factoryName = it.getString(ARG_FACTORY_NAME)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // 简化为运行时构造 ImageView（避免 binding 生成器依赖）
        val iv = ImageView(requireContext())
        iv.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        iv.scaleType = ImageView.ScaleType.FIT_CENTER
        _imageView = iv
        return iv
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val start = System.currentTimeMillis()
                val resp = withContext(Dispatchers.IO) {
                    buildApiClient().previewLabel(
                        LabelPreviewRequest(
                            type = labelType,
                            data = LabelPreviewData(
                                qrContent = qrContent,
                                lines = lines,
                                factoryName = factoryName
                            ),
                            format = "PNG"
                        ),
                        tenantId = 1L
                    )
                }
                val ms = System.currentTimeMillis() - start
                // TC-12.3.4.2：android-impl Fragment 启动 < 1s
                if (ms > 1000) {
                    android.util.Log.w(TAG, "LabelPreview 渲染耗时 ${ms}ms > 1s")
                }
                // base64 → Bitmap 显示
                val payload = resp.base64.substringAfter("base64,", resp.base64)
                val bytes = Base64.decode(payload, Base64.DEFAULT)
                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                _imageView?.setImageBitmap(bmp)
                Toast.makeText(
                    requireContext(),
                    "标签已渲染 · ${resp.sizeBytes}B · ${ms}ms",
                    Toast.LENGTH_SHORT
                ).show()
            } catch (e: Exception) {
                android.util.Log.e(TAG, "LabelPreview 渲染失败", e)
                Toast.makeText(
                    requireContext(),
                    "渲染失败：${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _imageView = null
    }

    /**
     * 构造 Retrofit ApiClient（与现有 v138 复用同一 baseUrl）
     */
    private fun buildApiClient(): ApiClient {
        return Retrofit.Builder()
            .baseUrl("https://erp.yourcompany.local/")
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(ApiClient::class.java)
    }

    companion object {
        const val TAG = "LabelPreviewFragment"
        const val ARG_TYPE = "type"
        const val ARG_QR_CONTENT = "qrContent"
        const val ARG_LINES = "lines"
        const val ARG_FACTORY_NAME = "factoryName"

        fun newInstance(
            type: String,
            qrContent: String,
            lines: List<String> = emptyList(),
            factoryName: String? = null
        ): LabelPreviewFragment {
            return LabelPreviewFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_TYPE, type)
                    putString(ARG_QR_CONTENT, qrContent)
                    putStringArrayList(ARG_LINES, ArrayList(lines))
                    putString(ARG_FACTORY_NAME, factoryName)
                }
            }
        }
    }
}