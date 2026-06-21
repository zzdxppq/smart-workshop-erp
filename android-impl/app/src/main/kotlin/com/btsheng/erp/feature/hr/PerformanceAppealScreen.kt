package com.btsheng.erp.feature.hr

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.btsheng.erp.core.network.PerformanceDto
import com.btsheng.erp.ui.theme.ErpColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerformanceAppealScreen(
    onBack: () -> Unit,
    viewModel: HrViewModel = hiltViewModel(),
) {
    val performances by viewModel.performances.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()
    val message by viewModel.message.collectAsState()
    var appealTarget by remember { mutableStateOf<PerformanceDto?>(null) }
    var reason by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { viewModel.loadPerformances() }

    if (appealTarget != null) {
        AlertDialog(
            onDismissRequest = { appealTarget = null },
            title = { Text("绩效申诉") },
            text = {
                Column {
                    Text("${appealTarget?.periodYear}-${appealTarget?.periodMonth} · 分数 ${appealTarget?.score} · 等级 ${appealTarget?.grade}")
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = reason, onValueChange = { reason = it }, label = { Text("申诉理由") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val id = appealTarget?.id ?: return@TextButton
                    viewModel.submitAppeal(id, reason) { appealTarget = null; reason = "" }
                }) { Text("提交") }
            },
            dismissButton = { TextButton(onClick = { appealTarget = null }) { Text("取消") } },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("我的绩效") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).padding(16.dp)) {
            if (loading) CircularProgressIndicator()
            error?.let { Text(it, color = ErpColors.ErrorRed) }
            message?.let { Text(it, color = ErpColors.IndustrialBlue) }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(performances) { perf ->
                    OutlinedCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text("${perf.periodYear}-${perf.periodMonth} · ${perf.grade} · ${perf.score}分")
                            Text(perf.kpiItems.orEmpty(), style = MaterialTheme.typography.bodySmall)
                            TextButton(onClick = { appealTarget = perf; reason = "" }) { Text("申诉") }
                        }
                    }
                }
            }
        }
    }
}
