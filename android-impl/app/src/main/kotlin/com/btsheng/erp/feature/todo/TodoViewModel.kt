package com.btsheng.erp.feature.todo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.btsheng.erp.core.auth.AppRoleSpec
import com.btsheng.erp.core.security.SessionStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TodoViewModel @Inject constructor(
    private val todoRepository: RoleTodoRepository,
) : ViewModel() {

    private val _items = MutableStateFlow<List<TodoItem>>(emptyList())
    val items: StateFlow<List<TodoItem>> = _items

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading

    private val _emptyHint = MutableStateFlow("")
    val emptyHint: StateFlow<String> = _emptyHint

    init {
        reload()
    }

    fun reload() {
        viewModelScope.launch {
            _loading.value = true
            val roles = SessionStore.roles()
            _emptyHint.value = AppRoleSpec.todoEmptyHint(roles)
            _items.value = todoRepository.load(roles)
            _loading.value = false
        }
    }
}

data class TodoItem(
    val category: String,
    val title: String,
    val subtitle: String,
    val action: TodoAction = TodoAction.NONE,
    val actionId: Long? = null,
    val payload: String? = null,
)
