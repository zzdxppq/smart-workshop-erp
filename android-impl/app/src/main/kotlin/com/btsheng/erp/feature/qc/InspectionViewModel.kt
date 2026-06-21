package com.btsheng.erp.feature.qc

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.btsheng.erp.core.network.ConcessionApprovalDto
import com.btsheng.erp.core.network.InspectionDetailDto
import com.btsheng.erp.core.network.InspectionListItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InspectionViewModel @Inject constructor(
    private val repository: QcInspectionRepository,
) : ViewModel() {

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading

    private val _submitting = MutableStateFlow(false)
    val submitting: StateFlow<Boolean> = _submitting

    private val _detail = MutableStateFlow<InspectionDetailDto?>(null)
    val detail: StateFlow<InspectionDetailDto?> = _detail

    private val _items = MutableStateFlow<List<EditableInspectionItem>>(emptyList())
    val items: StateFlow<List<EditableInspectionItem>> = _items

    private val _editable = MutableStateFlow(false)
    val editable: StateFlow<Boolean> = _editable

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    private var inspectionId: Long = 0L

    fun load(id: Long) {
        inspectionId = id
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            repository.getDetail(id).onSuccess { d ->
                _detail.value = d
                _items.value = d.toEditableItems()
                _editable.value = d.isEditable()
            }.onFailure {
                _error.value = it.message
            }
            _loading.value = false
        }
    }

    fun updateItem(index: Int, block: (EditableInspectionItem) -> EditableInspectionItem) {
        val list = _items.value.toMutableList()
        if (index in list.indices) {
            list[index] = block(list[index])
            _items.value = list
        }
    }

    fun submit(params: InspectionSubmitParams) {
        if (inspectionId <= 0L) return
        val current = _items.value
        if (current.any { it.measuredValue.isBlank() }) {
            _error.value = "请填写全部检验项实测值"
            return
        }
        if (params.overallResult == SubmitOverallResult.FAIL) {
            if (params.disposition.isNullOrBlank()) {
                _error.value = "请选择处置方式"
                return
            }
            if (params.defectQty == null || params.defectQty <= 0) {
                _error.value = "请填写不良数量"
                return
            }
        }
        if (params.overallResult == SubmitOverallResult.CONDITIONAL && params.conditionalReason.isNullOrBlank()) {
            _error.value = "请填写让步原因"
            return
        }
        viewModelScope.launch {
            _submitting.value = true
            _error.value = null
            repository.submit(inspectionId, current, params)
                .onSuccess { msg ->
                    _message.value = msg
                    _editable.value = false
                    load(inspectionId)
                }
                .onFailure { _error.value = it.message }
            _submitting.value = false
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}

@HiltViewModel
class ConcessionApprovalViewModel @Inject constructor(
    private val repository: QcInspectionRepository,
) : ViewModel() {

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading

    private val _rows = MutableStateFlow<List<InspectionListItem>>(emptyList())
    val rows: StateFlow<List<InspectionListItem>> = _rows

    private val _approvals = MutableStateFlow<List<ConcessionApprovalDto>>(emptyList())
    val approvals: StateFlow<List<ConcessionApprovalDto>> = _approvals

    private val _submitting = MutableStateFlow(false)
    val submitting: StateFlow<Boolean> = _submitting

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun loadList() {
        viewModelScope.launch {
            _loading.value = true
            _rows.value = repository.listConcessionPending()
            _loading.value = false
        }
    }

    fun loadApprovals(inspectionId: Long) {
        viewModelScope.launch {
            repository.getConcessionApprovals(inspectionId).onSuccess {
                _approvals.value = it
            }.onFailure {
                _approvals.value = emptyList()
            }
        }
    }

    fun approve(inspectionId: Long, approverRole: String, action: String, comment: String?) {
        viewModelScope.launch {
            _submitting.value = true
            _error.value = null
            repository.approveConcession(inspectionId, approverRole, action, comment)
                .onSuccess { _message.value = it }
                .onFailure { _error.value = it.message }
            _submitting.value = false
            loadList()
        }
    }

    fun clearMessage() { _message.value = null }
    fun clearError() { _error.value = null }
}
