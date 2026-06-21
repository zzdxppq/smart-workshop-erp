package com.btsheng.erp.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.btsheng.erp.core.network.AuthApi
import com.btsheng.erp.core.network.SysParamApi
import com.btsheng.erp.core.network.UserProfileDto
import com.btsheng.erp.core.update.AppVersionChecker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authApi: AuthApi,
    private val sysParamApi: SysParamApi,
) : ViewModel() {

    private val _profile = MutableStateFlow<UserProfileDto?>(null)
    val profile: StateFlow<UserProfileDto?> = _profile

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _checkingUpdate = MutableStateFlow(false)
    val checkingUpdate: StateFlow<Boolean> = _checkingUpdate

    private val _updateInfo = MutableStateFlow<AppVersionChecker.UpdateInfo?>(null)
    val updateInfo: StateFlow<AppVersionChecker.UpdateInfo?> = _updateInfo

    private val _updateMessage = MutableStateFlow<String?>(null)
    val updateMessage: StateFlow<String?> = _updateMessage

    fun loadProfile() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                val resp = authApi.me()
                if (resp.ok && resp.data != null) {
                    _profile.value = resp.data
                } else {
                    _error.value = resp.message ?: "加载失败"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "网络错误"
            } finally {
                _loading.value = false
            }
        }
    }

    fun checkForUpdate(manual: Boolean = true) {
        viewModelScope.launch {
            _checkingUpdate.value = true
            _updateMessage.value = null
            try {
                val info = AppVersionChecker.check(sysParamApi)
                _updateInfo.value = info
                if (info == null && manual) {
                    _updateMessage.value = "当前已是最新版本"
                }
            } catch (_: Exception) {
                if (manual) _updateMessage.value = "检查失败，请稍后重试"
            } finally {
                _checkingUpdate.value = false
            }
        }
    }

    fun dismissUpdateDialog() {
        _updateInfo.value = null
    }
}
