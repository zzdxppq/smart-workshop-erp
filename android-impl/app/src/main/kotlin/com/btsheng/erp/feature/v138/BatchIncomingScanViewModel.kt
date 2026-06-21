package com.btsheng.erp.feature.v138

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class BatchIncomingScanViewModel @Inject constructor(
    private val api: ApiClient,
) : ViewModel() {

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _poStatusText = MutableStateFlow<String?>(null)
    val poStatusText: StateFlow<String?> = _poStatusText

    private val _resultText = MutableStateFlow<String?>(null)
    val resultText: StateFlow<String?> = _resultText

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun loadPoStatus(poId: Long) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                val resp = api.getPoStatus(poId)
                if (!resp.ok || resp.data == null) {
                    _error.value = resp.message ?: "查询 PO 失败"
                    _poStatusText.value = null
                    return@launch
                }
                val d = resp.data!!
                val lines = d.items.orEmpty().joinToString("\n") { item ->
                    "物料 ${item.materialId}: 订 ${item.ordered} / 到 ${item.arrived} / 批 ${item.batchCount} · ${item.qualityStatus}"
                }
                _poStatusText.value = "PO ${d.poId} · ${d.poStatus}\n$lines"
            } catch (e: Exception) {
                _error.value = e.message ?: "网络错误"
            } finally {
                _loading.value = false
            }
        }
    }

    fun submitBatch(poId: Long, materialLines: String) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            _resultText.value = null
            try {
                val items = parseItems(materialLines)
                if (items.isEmpty()) {
                    _error.value = "物料清单格式：每行 materialId,quantity"
                    return@launch
                }
                val req = BatchCreateRequest(
                    poId = poId,
                    arrivedAt = LocalDateTime.now(),
                    items = items,
                )
                val resp = api.batchCreate(req)
                if (!resp.ok || resp.data == null) {
                    _error.value = resp.message ?: "提交批次失败"
                    return@launch
                }
                val d = resp.data!!
                val batches = d.batches.orEmpty().joinToString("\n") { "· ${it.batchNo} ×${it.quantity}" }
                val qc = d.qualityOrders.orEmpty().joinToString(", ")
                _resultText.value = buildString {
                    append("PO 状态：${d.poStatusAfter}\n")
                    append("批次：\n$batches\n")
                    if (qc.isNotBlank()) append("来料检：$qc")
                }
                loadPoStatus(poId)
            } catch (e: Exception) {
                _error.value = e.message ?: "网络错误"
            } finally {
                _loading.value = false
            }
        }
    }

    private fun parseItems(raw: String): List<BatchItem> {
        return raw.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .mapNotNull { line ->
                val parts = line.split(",", "，", " ").map { it.trim() }.filter { it.isNotEmpty() }
                if (parts.size < 2) return@mapNotNull null
                val materialId = parts[0].toLongOrNull() ?: return@mapNotNull null
                val qty = parts[1].toIntOrNull() ?: return@mapNotNull null
                if (qty < 1) return@mapNotNull null
                BatchItem(materialId, qty)
            }
            .toList()
    }
}
