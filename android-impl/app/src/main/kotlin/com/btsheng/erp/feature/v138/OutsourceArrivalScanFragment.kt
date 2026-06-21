package com.btsheng.erp.feature.v138

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
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

/** V1.3.5 · E12-S2 · 仓管扫 WW- 委外到货（扫码 + 手动输入） */
@AndroidEntryPoint
class OutsourceArrivalScanFragment : Fragment() {

    private val viewModel: OutsourceArrivalScanViewModel by viewModels()

    private val scanLauncher = registerForActivityResult(ScanContract()) { result ->
        result.contents?.trim()?.takeIf { it.isNotEmpty() }?.let { applyScannedCode(it) }
    }

    private var etOutsourceNo: EditText? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = inflater.inflate(R.layout.fragment_v138_outsource_arrival, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val etOutsourceNo = view.findViewById<EditText>(R.id.etOutsourceNo)
        this.etOutsourceNo = etOutsourceNo
        val etActualQty = view.findViewById<EditText>(R.id.etActualQty)
        val etActualWeight = view.findViewById<EditText>(R.id.etActualWeight)
        val etRemark = view.findViewById<EditText>(R.id.etRemark)
        val tvOrderInfo = view.findViewById<TextView>(R.id.tvOrderInfo)
        val tvResult = view.findViewById<TextView>(R.id.tvResult)
        val tvError = view.findViewById<TextView>(R.id.tvError)
        val progress = view.findViewById<ProgressBar>(R.id.progress)

        arguments?.getString(ARG_OUTSOURCE_NO)?.let { applyScannedCode(it) }

        view.findViewById<Button>(R.id.btnScanCamera).setOnClickListener {
            scanLauncher.launch(BarcodeScanHelper.options("扫 WW- 委外单码 · 到货"))
        }

        view.findViewById<Button>(R.id.btnLoad).setOnClickListener {
            viewModel.loadOutsource(etOutsourceNo.text.toString())
        }

        view.findViewById<Button>(R.id.btnConfirm).setOnClickListener {
            val qty = etActualQty.text.toString().toIntOrNull() ?: 0
            if (qty < 1) {
                tvError.isVisible = true
                tvError.text = "实收数量至少为 1"
                return@setOnClickListener
            }
            viewModel.confirmArrival(
                etOutsourceNo.text.toString(),
                qty,
                etActualWeight.text.toString(),
                etRemark.text.toString(),
            )
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.loading.collect { progress.isVisible = it } }
                launch {
                    viewModel.order.collect { order ->
                        if (order == null) {
                            tvOrderInfo.isVisible = false
                            return@collect
                        }
                        tvOrderInfo.isVisible = true
                        tvOrderInfo.text = buildString {
                            append("${order.outsourceNo} · ${order.status}\n")
                            append("工单 ${order.workorderNo} · ${order.processName}\n")
                            append("厂商 ${order.supplierName} · 物料 ${order.materialCode}\n")
                            append("订单数量 ${order.qty}")
                        }
                        order.qty?.takeIf { it > 0 }?.let { etActualQty.setText(it.toString()) }
                    }
                }
                launch {
                    viewModel.resultText.collect { text ->
                        tvResult.isVisible = !text.isNullOrBlank()
                        tvResult.text = text
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

    /** 扫码或外部传入：填入单号并自动加载 */
    private fun applyScannedCode(raw: String) {
        val normalized = OutsourceArrivalScanViewModel.normalizeOutsourceNo(raw)
        etOutsourceNo?.setText(normalized)
        viewModel.loadOutsource(normalized)
    }

    companion object {
        const val ARG_OUTSOURCE_NO = "outsource_no"
    }
}
