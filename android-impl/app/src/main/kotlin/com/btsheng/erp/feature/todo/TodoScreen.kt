package com.btsheng.erp.feature.todo

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

/** Spec 5.4 Tab2 · 待办（按角色：开工/入库/待检/审批） */
@Composable
fun TodoScreen(
    viewModel: TodoViewModel = hiltViewModel(),
    onItemAction: (TodoItem) -> Unit = {},
) {
    val items by viewModel.items.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val emptyHint by viewModel.emptyHint.collectAsState()

    if (loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Text("待办", style = MaterialTheme.typography.headlineSmall)
        }
        if (items.isEmpty()) {
            item {
                Text(emptyHint, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        items(items) { item ->
            Card(
                onClick = { if (item.action != TodoAction.NONE) onItemAction(item) },
                modifier = Modifier.fillMaxWidth(),
                enabled = item.action != TodoAction.NONE,
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text("[${item.category}] ${item.title}", style = MaterialTheme.typography.titleMedium)
                    Text(item.subtitle, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
