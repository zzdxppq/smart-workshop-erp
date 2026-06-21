package com.btsheng.erp.feature.v138

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.btsheng.erp.R
import com.btsheng.erp.core.scan.BarcodeScanHelper
import com.journeyapps.barcodescanner.ScanContract
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * V1.3.8 · 物料码扫码 · 解析 + 入库/出库（E4-S2 · 扫码 + 手动输入）
 */
@AndroidEntryPoint
class MaterialBarcodeScanFragment : Fragment() {

    private val viewModel: MaterialBarcodeScanViewModel by viewModels()

    private val scanLauncher = registerForActivityResult(ScanContract()) { result ->
        result.contents?.trim()?.takeIf { it.isNotEmpty() }?.let { applyBarcode(it) }
    }

    private var etBarcode: EditText? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = inflater.inflate(R.layout.fragment_v138_material_barcode_scan, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mode = arguments?.getString(ARG_MODE)?.let {
            runCatching { MaterialScanMode.valueOf(it) }.getOrDefault(MaterialScanMode.PARSE_ONLY)
        } ?: MaterialScanMode.PARSE_ONLY

        val tvTitle = view.findViewById<TextView>(R.id.tvTitle)
        val tvHint = view.findViewById<TextView>(R.id.tvHint)
        val etBarcode = view.findViewById<EditText>(R.id.etBarcode)
        this.etBarcode = etBarcode
        val layoutResult = view.findViewById<LinearLayout>(R.id.layoutResult)
        val etLocation = view.findViewById<EditText>(R.id.etLocation)
        val etWorkorder = view.findViewById<EditText>(R.id.etWorkorder)
        val etQty = view.findViewById<EditText>(R.id.etQty)
        val btnSubmit = view.findViewById<Button>(R.id.btnSubmit)
        val tvOpResult = view.findViewById<TextView>(R.id.tvOpResult)
        val tvError = view.findViewById<TextView>(R.id.tvError)
        val progress = view.findViewById<ProgressBar>(R.id.progress)

        val scanPrompt = when (mode) {
            MaterialScanMode.INBOUND -> "扫 WL- 物料码 · 入库"
            MaterialScanMode.OUTBOUND -> "扫 WL- 物料码 · 出库"
            else -> "扫 WL- 物料码"
        }

        view.findViewById<Button>(R.id.btnScanCamera).setOnClickListener {
            scanLauncher.launch(BarcodeScanHelper.options(scanPrompt))
        }

        arguments?.getString(ARG_BARCODE)?.let { applyBarcode(it) }

        when (mode) {
            MaterialScanMode.INBOUND -> {
                tvTitle.text = "扫码入库（WL-）"
                tvHint.text = "可扫二维码，也可手动输入 WL- 物料码"
                etLocation.isVisible = true
                etQty.isVisible = true
                btnSubmit.isVisible = true
                btnSubmit.text = "确认入库"
            }
            MaterialScanMode.OUTBOUND -> {
                tvTitle.text = "扫码出库（WL-）"
                tvHint.text = "可扫二维码，也可手动输入 WL- 物料码"
                etWorkorder.isVisible = true
                etLocation.isVisible = true
                etLocation.hint = "库位（可选）"
                etQty.isVisible = true
                btnSubmit.isVisible = true
                btnSubmit.text = "确认出库"
            }
            MaterialScanMode.PARSE_ONLY -> {
                tvTitle.text = "物料码解析"
                tvHint.text = "可扫二维码，也可手动输入"
            }
        }

        view.findViewById<Button>(R.id.btnParse).setOnClickListener {
            val code = etBarcode.text.toString().trim()
            if (code.isEmpty()) {
                tvError.isVisible = true
                tvError.text = "请输入或扫描条码"
                return@setOnClickListener
            }
            viewModel.parseBarcode(code)
        }

        btnSubmit.setOnClickListener {
            val code = etBarcode.text.toString().trim()
            val qty = etQty.text.toString().toIntOrNull() ?: 1
            when (mode) {
                MaterialScanMode.INBOUND -> {
                    val loc = etLocation.text.toString().trim()
                    if (loc.isEmpty()) {
                        tvError.isVisible = true
                        tvError.text = "请填写库位"
                        return@setOnClickListener
                    }
                    viewModel.submitInbound(code, loc, qty)
                }
                MaterialScanMode.OUTBOUND -> {
                    val wo = etWorkorder.text.toString().trim()
                    if (wo.isEmpty()) {
                        tvError.isVisible = true
                        tvError.text = "请填写工单号"
                        return@setOnClickListener
                    }
                    viewModel.submitOutbound(code, wo, qty, etLocation.text.toString())
                }
                else -> Unit
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.loading.collect { progress.isVisible = it } }
                launch {
                    viewModel.parseResult.collect { data ->
                        if (data == null) {
                            layoutResult.isVisible = false
                            return@collect
                        }
                        layoutResult.isVisible = true
                        view.findViewById<TextView>(R.id.tvBarcode).text = "条码：${etBarcode.text}"
                        view.findViewById<TextView>(R.id.tvMaterialId).text = "物料 ID：${data.materialId}"
                        view.findViewById<TextView>(R.id.tvMaterialNo).text = "物料号：${data.materialNo}"
                        view.findViewById<TextView>(R.id.tvBatchId).text = "批次 ID：${data.batchId}"
                        view.findViewById<TextView>(R.id.tvBatchNo).text = "批次号：${data.batchNo}"
                        view.findViewById<TextView>(R.id.tvArrivedAt).text = "到货：${data.arrivedAt}"
                        view.findViewById<TextView>(R.id.tvQualityStatus).text = "质检：${data.qualityStatus}"
                    }
                }
                launch {
                    viewModel.opResult.collect { text ->
                        tvOpResult.isVisible = !text.isNullOrBlank()
                        tvOpResult.text = text
                    }
                }
                launch {
                    viewModel.error.collect { text ->
                        tvError.isVisible = !text.isNullOrBlank()
                        tvError.text = text
                    }
                }
            }
        }
    }

    private fun applyBarcode(code: String) {
        etBarcode?.setText(code)
        viewModel.parseBarcode(code)
    }

    companion object {
        const val ARG_MODE = "scan_mode"
        const val ARG_BARCODE = "barcode"
    }
}
