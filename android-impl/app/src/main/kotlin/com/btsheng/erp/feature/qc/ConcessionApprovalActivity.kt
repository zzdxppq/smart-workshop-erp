package com.btsheng.erp.feature.qc

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.btsheng.erp.ui.theme.ErpTheme
import dagger.hilt.android.AndroidEntryPoint

/** 让步接收双签审批（品质主管 + 生管） */
@AndroidEntryPoint
class ConcessionApprovalActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val inspectionId = intent.getLongExtra(EXTRA_INSPECTION_ID, 0L)
        setContent {
            ErpTheme {
                if (inspectionId > 0L) {
                    ConcessionApproveDetailScreen(inspectionId = inspectionId, onFinish = { finish() })
                } else {
                    ConcessionApprovalListScreen(onFinish = { finish() })
                }
            }
        }
    }

    companion object {
        const val EXTRA_INSPECTION_ID = "inspectionId"

        fun listIntent(context: Context) =
            Intent(context, ConcessionApprovalActivity::class.java)

        fun detailIntent(context: Context, inspectionId: Long) =
            Intent(context, ConcessionApprovalActivity::class.java).apply {
                putExtra(EXTRA_INSPECTION_ID, inspectionId)
            }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConcessionApprovalListScreen(
    viewModel: ConcessionApprovalViewModel = hiltViewModel(),
    onFinish: () -> Unit,
) {
    val loading by viewModel.loading.collectAsState()
    val rows by viewModel.rows.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(Unit) { viewModel.loadList() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("让步审批") },
                navigationIcon = { TextButton(onClick = onFinish) { Text("返回") } },
            )
        },
    ) { padding ->
        if (loading) {
            Box(Modifier.padding(padding).fillMaxSize()) {
                CircularProgressIndicator(Modifier.align(androidx.compose.ui.Alignment.Center))
            }
        } else {
            LazyColumn(
                Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(rows) { row ->
                    OutlinedCard(
                        onClick = {
                            row.id?.let { id ->
                                context.startActivity(ConcessionApprovalActivity.detailIntent(context, id))
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(row.inspectionNo ?: "检验单", style = MaterialTheme.typography.titleSmall)
                            Text("${row.type ?: "-"} · ${row.materialCode ?: "-"}")
                            Text("待审批", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConcessionApproveDetailScreen(
    inspectionId: Long,
    viewModel: ConcessionApprovalViewModel = hiltViewModel(),
    onFinish: () -> Unit,
) {
    val approvals by viewModel.approvals.collectAsState()
    val submitting by viewModel.submitting.collectAsState()
    val message by viewModel.message.collectAsState()
    val error by viewModel.error.collectAsState()
    var approverRole by remember { mutableStateOf("QUALITY_MANAGER") }
    var comment by remember { mutableStateOf("") }

    LaunchedEffect(inspectionId) { viewModel.loadApprovals(inspectionId) }

    message?.let { msg ->
        AlertDialog(
            onDismissRequest = { viewModel.clearMessage(); onFinish() },
            title = { Text("审批完成") },
            text = { Text(msg) },
            confirmButton = { TextButton(onClick = { viewModel.clearMessage(); onFinish() }) { Text("完成") } },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("让步双签") },
                navigationIcon = { TextButton(onClick = onFinish) { Text("返回") } },
            )
        },
        bottomBar = {
            Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { viewModel.approve(inspectionId, approverRole, "REJECT", comment) },
                    modifier = Modifier.weight(1f),
                    enabled = !submitting,
                ) { Text("驳回") }
                Button(
                    onClick = { viewModel.approve(inspectionId, approverRole, "APPROVE", comment) },
                    modifier = Modifier.weight(1f),
                    enabled = !submitting,
                ) { Text(if (submitting) "提交中…" else "通过") }
            }
        },
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Text("审批进度", style = MaterialTheme.typography.titleSmall)
            approvals.forEach { a ->
                Text("${a.approverRoleLabel ?: a.approverRole} · ${a.approvalStatus ?: "PENDING"}")
                a.comment?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            }
            Text("我的角色", style = MaterialTheme.typography.labelSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = approverRole == "QUALITY_MANAGER",
                    onClick = { approverRole = "QUALITY_MANAGER" },
                    label = { Text("品质主管") },
                )
                FilterChip(
                    selected = approverRole == "PRODUCTION_MANAGER",
                    onClick = { approverRole = "PRODUCTION_MANAGER" },
                    label = { Text("生管") },
                )
            }
            OutlinedTextField(
                value = comment,
                onValueChange = { comment = it },
                label = { Text("审批意见") },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
