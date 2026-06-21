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
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.btsheng.erp.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * V1.3.8 Sprint 8 Story 8.5 · 分批到货扫码 Fragment
 *
 * POST /incoming/batch-create · GET /incoming/po-status/{poId}
 */
@AndroidEntryPoint
class BatchIncomingScanFragment : Fragment() {

    private val viewModel: BatchIncomingScanViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = inflater.inflate(R.layout.fragment_v138_batch_incoming, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val etPoId = view.findViewById<EditText>(R.id.etPoId)
        val etMaterialList = view.findViewById<EditText>(R.id.etMaterialList)
        val tvPoStatus = view.findViewById<TextView>(R.id.tvPoStatus)
        val tvResult = view.findViewById<TextView>(R.id.tvResult)
        val tvError = view.findViewById<TextView>(R.id.tvError)
        val progress = view.findViewById<ProgressBar>(R.id.progress)

        arguments?.getLong(ARG_PO_ID)?.takeIf { it > 0L }?.let { etPoId.setText(it.toString()) }

        view.findViewById<Button>(R.id.btnLoadPo).setOnClickListener {
            val poId = etPoId.text.toString().toLongOrNull()
            if (poId == null) {
                tvError.isVisible = true
                tvError.text = "请输入有效 PO ID"
                return@setOnClickListener
            }
            viewModel.loadPoStatus(poId)
        }

        view.findViewById<Button>(R.id.btnSubmitBatch).setOnClickListener {
            val poId = etPoId.text.toString().toLongOrNull()
            if (poId == null) {
                tvError.isVisible = true
                tvError.text = "请输入有效 PO ID"
                return@setOnClickListener
            }
            viewModel.submitBatch(poId, etMaterialList.text.toString())
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.loading.collect { progress.isVisible = it }
                }
                launch {
                    viewModel.poStatusText.collect { text ->
                        tvPoStatus.isVisible = !text.isNullOrBlank()
                        tvPoStatus.text = text
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

        etPoId.text.toString().toLongOrNull()?.let { viewModel.loadPoStatus(it) }
    }

    companion object {
        const val ARG_PO_ID = "po_id"
    }
}
