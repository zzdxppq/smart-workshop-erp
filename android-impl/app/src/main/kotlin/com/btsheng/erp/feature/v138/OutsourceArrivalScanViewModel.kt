package com.btsheng.erp.feature.v138

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.btsheng.erp.core.network.OutsourceArriveBody
import com.btsheng.erp.core.network.OutsourceOrderDto
import com.btsheng.erp.core.network.OutsourceReceiveApi
import com.btsheng.erp.core.scan.QrCodeParser
import com.btsheng.erp.core.security.SessionStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

@HiltViewModel
class OutsourceArrivalScanViewModel @Inject constructor(
    private val api: OutsourceReceiveApi,
) : ViewModel() {

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _order = MutableStateFlow<OutsourceOrderDto?>(null)
    val order: StateFlow<OutsourceOrderDto?> = _order

    private val _resultText = MutableStateFlow<String?>(null)
    val resultText: StateFlow<String?> = _resultText

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun loadOutsource(rawCode: String) {
        val no = normalizeOutsourceNo(rawCode)
        if (no.isBlank()) {
            _error.value = "请输入 WW- 委外单码"
            return
        }
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            _resultText.value = null
            try {
                val resp = api.getOutsource(no)
                if (!resp.ok || resp.data == null) {
                    _error.value = resp.message ?: "委外单不存在"
                    _order.value = null
                    return@launch
                }
                _order.value = resp.data
            } catch (e: Exception) {
                _error.value = e.message ?: "网络错误"
            } finally {
                _loading.value = false
            }
        }
    }

    fun confirmArrival(outsourceNo: String, actualQty: Int, actualWeight: String?, remark: String?) {
        val no = normalizeOutsourceNo(outsourceNo)
        val userId = SessionStore.session?.userId ?: 1L
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            _resultText.value = null
            try {
                val weight = actualWeight?.trim()?.toBigDecimalOrNull()
                val body = OutsourceArriveBody(
                    outsourceNo = no,
                    actualQty = actualQty,
                    actualWeight = weight,
                    remark = remark?.takeIf { it.isNotBlank() },
                )
                val resp = api.receiveByNo(no, body, userId)
                if (!resp.ok || resp.data == null) {
                    _error.value = resp.message ?: "到货确认失败"
                    return@launch
                }
                val d = resp.data!!
                _order.value = d
                _resultText.value = buildString {
                    append("到货成功\n")
                    append("${d.outsourceNo} → ${d.status}\n")
                    append("已通知生管 + 品质（待检）")
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "网络错误"
            } finally {
                _loading.value = false
            }
        }
    }

    companion object {
        fun normalizeOutsourceNo(raw: String): String {
            val trimmed = raw.trim().uppercase()
            if (trimmed.isBlank()) return ""
            val parsed = QrCodeParser.parse(trimmed)
            if (parsed.type == QrCodeParser.TYPE_OUTSOURCE_ORDER && !parsed.code.isNullOrBlank()) {
                return normalizeOutsourceNo(parsed.code!!)
            }
            if (trimmed.matches(Regex("^WW-\\d{8}-\\d{4}$"))) {
                return "WW" + trimmed.substring(3)
            }
            return trimmed
        }

        fun isOutsourceCode(raw: String): Boolean {
            val t = raw.trim().uppercase()
            return t.matches(Regex("^WW-?\\d{8}-\\d{4}$")) ||
                QrCodeParser.parse(t).type == QrCodeParser.TYPE_OUTSOURCE_ORDER
        }
    }
}
