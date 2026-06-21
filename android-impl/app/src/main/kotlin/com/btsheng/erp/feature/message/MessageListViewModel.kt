package com.btsheng.erp.feature.message

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.btsheng.erp.core.network.AppMessageApi
import com.btsheng.erp.core.network.MarkReadRequest
import com.btsheng.erp.core.auth.AppRoleSpec
import com.btsheng.erp.core.security.SessionStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MessageListViewModel @Inject constructor(
    private val messageApi: AppMessageApi,
) : ViewModel() {

    private val _messages = MutableStateFlow<List<MessageViewModel.AppMessage>>(emptyList())
    val messages: StateFlow<List<MessageViewModel.AppMessage>> = _messages

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    init {
        reload()
    }

    fun reload() {
        viewModelScope.launch {
            _loading.value = true
            try {
                val resp = messageApi.listMessages(unreadOnly = false)
                if (resp.ok && resp.data != null) {
                    _messages.value = resp.data.mapIndexed { index, it ->
                        MessageViewModel.AppMessage(
                            id = it.id ?: 0L,
                            type = it.type ?: "INFO",
                            title = it.title ?: "",
                            content = it.content ?: "",
                            routeUrl = it.routeUrl ?: "",
                            read = it.read ?: false,
                            timeLabel = sampleTime(index),
                        )
                    }
                }
            } catch (_: Exception) {
                val allowed = AppRoleSpec.sampleMessageTypes(SessionStore.roles())
                _messages.value = listOf(
                    MessageViewModel.AppMessage(
                        1, "APPROVAL_NOTIFY", "审批通知", "报价 BJ001 待审批",
                        "approval/detail/100", false, "刚刚",
                    ),
                    MessageViewModel.AppMessage(
                        2, "OVERDUE_NOTIFY", "逾期提醒", "审批单 AP002 已逾期 24h",
                        "approval/pending", false, "2 小时前",
                    ),
                    MessageViewModel.AppMessage(
                        3, "SCAN_RECEIPT", "扫码回执", "工单 GD-20260615-0001 开工成功",
                        "scan/history", true, "昨天",
                    ),
                    MessageViewModel.AppMessage(
                        4, "EXCEPTION_REPORT", "异常上报", "委外 WW-001 到货数量差异 8%",
                        "warehouse/arrival", false, "3 小时前",
                    ),
                ).filter { it.type in allowed }
            } finally {
                _loading.value = false
            }
        }
    }

    fun markRead(messageId: Long) {
        _messages.value = _messages.value.map {
            if (it.id == messageId) it.copy(read = true) else it
        }
        viewModelScope.launch {
            try {
                messageApi.markRead(messageId, MarkReadRequest(SessionStore.session?.userId ?: 1L))
            } catch (_: Exception) {
                /* 离线时本地标记已读 */
            }
        }
    }

    private fun sampleTime(index: Int) = when (index) {
        0 -> "刚刚"
        1 -> "2 小时前"
        else -> "昨天"
    }
}
