package com.btsheng.erp.feature.scan

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.btsheng.erp.core.auth.AppRoleSpec
import com.btsheng.erp.core.security.SessionStore
import com.btsheng.erp.core.security.TokenStore
import com.btsheng.erp.feature.v138.ApiClient
import com.btsheng.erp.feature.v139.DrawingFeatureFlag
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class RoleScanUiState {
    data object Idle : RoleScanUiState()
    data object Loading : RoleScanUiState()
    data class Message(val text: String) : RoleScanUiState()
    data class Progress(val info: WorkorderProgressInfo) : RoleScanUiState()
}

@HiltViewModel
class ScanViewModel @Inject constructor(
    private val repository: ScanRepository,
    private val roleScanRepository: RoleScanRepository,
    private val apiClient: ApiClient,
) : ViewModel() {

    var pendingCount by mutableIntStateOf(0)
        private set
    var isOnline by mutableStateOf(true)
        private set
    var lastMessage by mutableStateOf<String?>(null)
        private set
    var drawingGrayEnabled by mutableStateOf(false)
        private set
    var roleScanState by mutableStateOf<RoleScanUiState>(RoleScanUiState.Idle)
        private set

    init {
        refreshPending()
        refreshDrawingGray()
    }

    fun refreshPending() {
        viewModelScope.launch {
            pendingCount = repository.pendingCount()
        }
    }

    fun refreshDrawingGray() {
        viewModelScope.launch {
            val token = SessionStore.session?.accessToken
                ?: TokenStore.load()?.accessToken
                ?: return@launch
            try {
                val flag = DrawingFeatureFlag(apiClient, "Bearer $token")
                drawingGrayEnabled = flag.isOperatorGrayEnabled("OPERATOR")
            } catch (_: Exception) {
                drawingGrayEnabled = false
            }
        }
    }

    fun clearRoleScanState() {
        roleScanState = RoleScanUiState.Idle
    }

    fun handleRoleScan(rawCode: String, role: AppRoleSpec.PrimaryRole, onInspection: (Long) -> Unit) {
        viewModelScope.launch {
            roleScanState = RoleScanUiState.Loading
            try {
                when (role) {
                    AppRoleSpec.PrimaryRole.QC -> {
                        val r = roleScanRepository.resolveQcScan(rawCode)
                        if (r.inspectionId != null) {
                            roleScanState = RoleScanUiState.Idle
                            onInspection(r.inspectionId)
                        } else {
                            roleScanState = RoleScanUiState.Message(r.message ?: "未找到待检单")
                        }
                    }
                    AppRoleSpec.PrimaryRole.PROD_MGR -> {
                        val info = roleScanRepository.lookupWorkorderProgress(rawCode)
                        roleScanState = RoleScanUiState.Progress(info)
                    }
                    AppRoleSpec.PrimaryRole.OPERATOR -> {
                        roleScanState = RoleScanUiState.Idle
                    }
                    else -> roleScanState = RoleScanUiState.Message("当前角色不支持此扫码")
                }
            } catch (e: Exception) {
                roleScanState = RoleScanUiState.Message(e.message ?: "扫码处理失败")
            }
        }
    }

    fun submitStep(
        step: Int,
        code: String,
        workorderCode: String,
        qtyDone: Int,
        qtyOk: Int,
        qtyScrap: Int,
        onDone: (Boolean) -> Unit,
    ) {
        viewModelScope.launch {
            val result = repository.submitThreeCodeStep(step, code, workorderCode, qtyDone, qtyOk, qtyScrap)
            result.onSuccess {
                isOnline = true
                lastMessage = when (step) {
                    0 -> "开工成功"
                    1 -> "过站成功"
                    else -> "报工已提交"
                }
                refreshPending()
                onDone(true)
            }.onFailure {
                isOnline = false
                lastMessage = it.message ?: "已入离线队列"
                refreshPending()
                onDone(false)
            }
        }
    }
}
