package com.btsheng.erp.feature.hr

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.btsheng.erp.BuildConfig
import com.btsheng.erp.core.network.PayrollDto
import com.btsheng.erp.core.network.PerformanceDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HrViewModel @Inject constructor(private val repository: HrRepository) : ViewModel() {

    private val _payrolls = MutableStateFlow<List<PayrollDto>>(emptyList())
    val payrolls: StateFlow<List<PayrollDto>> = _payrolls

    private val _performances = MutableStateFlow<List<PerformanceDto>>(emptyList())
    val performances: StateFlow<List<PerformanceDto>> = _performances

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    fun loadPayrolls() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                _payrolls.value = repository.loadPayrolls()
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _loading.value = false
            }
        }
    }

    fun loadPerformances() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                _performances.value = repository.loadPerformances()
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _loading.value = false
            }
        }
    }

    fun submitAppeal(performanceId: Long, reason: String, onDone: () -> Unit) {
        viewModelScope.launch {
            _error.value = null
            try {
                repository.submitAppeal(performanceId, reason)
                _message.value = "申诉已提交"
                onDone()
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun payslipPdfUrl(payrollId: Long): String {
        val base = BuildConfig.API_BASE_URL.ifBlank { "http://10.0.2.2:9080" }
        return repository.payslipPdfUrl(payrollId, base)
    }
}
