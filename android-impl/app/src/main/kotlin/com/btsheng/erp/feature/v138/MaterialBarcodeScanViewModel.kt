package com.btsheng.erp.feature.v138

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.btsheng.erp.core.network.ScanInboundBody
import com.btsheng.erp.core.network.ScanOutboundBody
import com.btsheng.erp.core.network.WarehouseScanApi
import com.btsheng.erp.core.security.SessionStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class MaterialScanMode { INBOUND, OUTBOUND, PARSE_ONLY }

@HiltViewModel
class MaterialBarcodeScanViewModel @Inject constructor(
    private val apiClient: ApiClient,
    private val warehouseScanApi: WarehouseScanApi,
) : ViewModel() {

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _parseResult = MutableStateFlow<MaterialBarcodeParseResponse?>(null)
    val parseResult: StateFlow<MaterialBarcodeParseResponse?> = _parseResult

    private val _opResult = MutableStateFlow<String?>(null)
    val opResult: StateFlow<String?> = _opResult

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun parseBarcode(barcode: String) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            _parseResult.value = null
            try {
                val resp = apiClient.parseMaterialBarcode(barcode.trim())
                if (!resp.ok || resp.data == null) {
                    _error.value = resp.message ?: "解析失败"
                    return@launch
                }
                _parseResult.value = resp.data
            } catch (e: Exception) {
                _error.value = e.message ?: "网络错误"
            } finally {
                _loading.value = false
            }
        }
    }

    fun submitInbound(barcode: String, locationCode: String, qty: Int) {
        val userId = SessionStore.session?.userId ?: 1L
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            _opResult.value = null
            try {
                val resp = warehouseScanApi.scanInbound(
                    ScanInboundBody(barcodeNo = barcode.trim(), locationCode = locationCode, qty = qty),
                    userId,
                )
                if (!resp.ok || resp.data == null) {
                    _error.value = resp.message ?: "入库失败"
                    return@launch
                }
                val d = resp.data!!
                _opResult.value = "入库成功 · ${d.scanNo} · ${d.materialCode} ×${d.qty}"
            } catch (e: Exception) {
                _error.value = e.message ?: "网络错误"
            } finally {
                _loading.value = false
            }
        }
    }

    fun submitOutbound(barcode: String, workorderNo: String, qty: Int, locationCode: String?) {
        val userId = SessionStore.session?.userId ?: 1L
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            _opResult.value = null
            try {
                val resp = warehouseScanApi.scanOutbound(
                    ScanOutboundBody(
                        barcodeNo = barcode.trim(),
                        workorderNo = workorderNo.trim(),
                        qty = qty,
                        locationCode = locationCode?.takeIf { it.isNotBlank() },
                    ),
                    userId,
                )
                if (!resp.ok || resp.data == null) {
                    _error.value = resp.message ?: "出库失败"
                    return@launch
                }
                val d = resp.data!!
                _opResult.value = "出库成功 · ${d.scanNo} · ${d.materialCode} ×${d.qty}"
            } catch (e: Exception) {
                _error.value = e.message ?: "网络错误"
            } finally {
                _loading.value = false
            }
        }
    }
}
