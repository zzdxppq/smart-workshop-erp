package com.btsheng.erp.feature.hr

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.btsheng.erp.core.network.PayrollDto
import com.btsheng.erp.ui.theme.ErpColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PayslipScreen(
    onBack: () -> Unit,
    viewModel: HrViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val payrolls by viewModel.payrolls.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadPayrolls() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("我的工资") },
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
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(payrolls) { p ->
                    PayslipCard(p) {
                        val url = viewModel.payslipPdfUrl(p.id ?: return@PayslipCard)
                        context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url)))
                    }
                }
            }
            if (!loading && payrolls.isEmpty()) {
                Text("暂无工资记录", color = ErpColors.TextSecondary)
            }
        }
    }
}

@Composable
private fun PayslipCard(p: PayrollDto, onOpenPdf: () -> Unit) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("${p.periodYear}-${p.periodMonth?.toString()?.padStart(2, '0') ?: "--"}", style = MaterialTheme.typography.titleMedium)
            Text("实发：¥${p.netSalary ?: 0}")
            Text("基本 ${p.baseSalary ?: 0} · 计件 ${p.piecePay ?: 0} · 绩效 ${p.performanceBonus ?: 0}", style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = onOpenPdf) { Text("查看 PDF 工资条") }
        }
    }
}
