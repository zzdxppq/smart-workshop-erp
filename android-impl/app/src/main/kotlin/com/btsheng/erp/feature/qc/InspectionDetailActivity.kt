package com.btsheng.erp.feature.qc

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.btsheng.erp.ui.theme.ErpColors
import com.btsheng.erp.ui.theme.ErpTheme
import dagger.hilt.android.AndroidEntryPoint

/** 品检主力 · 完整检验录入与提交（IQC/IPQC/OQC） */
@AndroidEntryPoint
class InspectionDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val id = intent.getLongExtra(EXTRA_INSPECTION_ID, 0L)
        if (id <= 0L) {
            finish()
            return
        }
        setContent {
            ErpTheme {
                InspectionExecuteScreen(
                    inspectionId = id,
                    onFinish = { finish() },
                )
            }
        }
    }

    companion object {
        const val EXTRA_INSPECTION_ID = "inspectionId"

        fun intent(context: Context, inspectionId: Long) =
            Intent(context, InspectionDetailActivity::class.java).apply {
                putExtra(EXTRA_INSPECTION_ID, inspectionId)
            }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InspectionExecuteScreen(
    inspectionId: Long,
    viewModel: InspectionViewModel = hiltViewModel(),
    onFinish: () -> Unit,
) {
    val loading by viewModel.loading.collectAsState()
    val submitting by viewModel.submitting.collectAsState()
    val detail by viewModel.detail.collectAsState()
    val items by viewModel.items.collectAsState()
    val editable by viewModel.editable.collectAsState()
    val error by viewModel.error.collectAsState()
    val message by viewModel.message.collectAsState()

    var remark by remember { mutableStateOf("") }
    var showSubmitDialog by remember { mutableStateOf(false) }
    var overallResult by remember { mutableStateOf(SubmitOverallResult.PASS) }
    var disposition by remember { mutableStateOf("RETURN") }
    var defectQty by remember { mutableStateOf("1") }
    var conditionalReason by remember { mutableStateOf("") }
    var rejectReason by remember { mutableStateOf("") }

    LaunchedEffect(inspectionId) { viewModel.load(inspectionId) }

    message?.let { msg ->
        AlertDialog(
            onDismissRequest = { viewModel.clearMessage(); onFinish() },
            title = { Text("提交成功") },
            text = { Text(msg) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearMessage(); onFinish() }) { Text("完成") }
            },
        )
    }

    if (showSubmitDialog) {
        AlertDialog(
            onDismissRequest = { showSubmitDialog = false },
            title = { Text("提交检验判定") },
            text = {
                Column(
                    Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("总体判定")
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = overallResult == SubmitOverallResult.PASS,
                            onClick = { overallResult = SubmitOverallResult.PASS },
                            label = { Text("合格") },
                        )
                        FilterChip(
                            selected = overallResult == SubmitOverallResult.FAIL,
                            onClick = { overallResult = SubmitOverallResult.FAIL },
                            label = { Text("不合格") },
                        )
                        FilterChip(
                            selected = overallResult == SubmitOverallResult.CONDITIONAL,
                            onClick = { overallResult = SubmitOverallResult.CONDITIONAL },
                            label = { Text("让步") },
                        )
                    }
                    when (overallResult) {
                        SubmitOverallResult.FAIL -> {
                            Text("处置方式", style = MaterialTheme.typography.labelSmall)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf("RETURN" to "退货", "REWORK" to "返工", "SCRAP" to "报废").forEach { (v, label) ->
                                    FilterChip(
                                        selected = disposition == v,
                                        onClick = { disposition = v },
                                        label = { Text(label) },
                                    )
                                }
                            }
                            OutlinedTextField(
                                value = defectQty,
                                onValueChange = { defectQty = it.filter { c -> c.isDigit() } },
                                label = { Text("不良数量") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                            )
                            OutlinedTextField(
                                value = rejectReason,
                                onValueChange = { rejectReason = it },
                                label = { Text("不合格原因") },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        SubmitOverallResult.CONDITIONAL -> {
                            OutlinedTextField(
                                value = conditionalReason,
                                onValueChange = { conditionalReason = it },
                                label = { Text("让步原因") },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        else -> Unit
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showSubmitDialog = false
                    viewModel.submit(
                        InspectionSubmitParams(
                            overallResult = overallResult,
                            disposition = if (overallResult == SubmitOverallResult.FAIL) disposition else null,
                            defectQty = if (overallResult == SubmitOverallResult.FAIL) defectQty.toIntOrNull() else null,
                            conditionalReason = if (overallResult == SubmitOverallResult.CONDITIONAL) conditionalReason else null,
                            remark = remark.takeIf { it.isNotBlank() },
                            rejectReason = rejectReason.takeIf { it.isNotBlank() },
                        ),
                    )
                }) { Text("确认提交") }
            },
            dismissButton = {
                TextButton(onClick = { showSubmitDialog = false }) { Text("取消") }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (editable) "检验录入" else "检验详情") },
                navigationIcon = { TextButton(onClick = onFinish) { Text("返回") } },
            )
        },
        bottomBar = {
            if (editable && !loading) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                ) {
                    Button(
                        onClick = { showSubmitDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !submitting,
                    ) { Text(if (submitting) "提交中…" else "提交检验报告") }
                }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when {
                loading -> CircularProgressIndicator(Modifier.align(androidx.compose.ui.Alignment.Center))
                detail == null -> Text(error ?: "加载失败", Modifier.padding(16.dp))
                else -> {
                    val d = detail!!
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        item {
                            Text(d.inspectionNo ?: "检验单", style = MaterialTheme.typography.headlineSmall)
                            Text("类型 ${d.type ?: "-"} · ${d.result ?: "待检"}")
                            d.workOrderNo?.let { Text("工单：$it ${d.processName.orEmpty()}") }
                            Text("物料：${d.materialCode ?: "-"} ${d.materialName.orEmpty()}")
                            if (editable) {
                                Text(
                                    "手机端录入实测值 · 逐项判定后提交",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ErpColors.IndustrialBlue,
                                )
                            }
                            error?.let {
                                Text(it, color = ErpColors.ErrorRed, style = MaterialTheme.typography.bodySmall)
                            }
                            Spacer(Modifier.height(4.dp))
                        }
                        itemsIndexed(items) { index, item ->
                            InspectionItemCard(
                                item = item,
                                editable = editable,
                                onMeasuredChange = { v ->
                                    viewModel.updateItem(index) { it.copy(measuredValue = v) }
                                },
                                onPassChange = { pass ->
                                    viewModel.updateItem(index) {
                                        it.copy(passed = pass, severity = if (pass) "INFO" else "ERROR")
                                    }
                                },
                                onSeverityChange = { sev ->
                                    viewModel.updateItem(index) { it.copy(severity = sev) }
                                },
                                onDefectChange = { desc ->
                                    viewModel.updateItem(index) { it.copy(defectDesc = desc) }
                                },
                            )
                        }
                        if (editable) {
                            item {
                                OutlinedTextField(
                                    value = remark,
                                    onValueChange = { remark = it },
                                    label = { Text("备注（可选）") },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        } else {
                            item {
                                Text(
                                    "检验已关闭 · PC 端可查看报告归档",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InspectionItemCard(
    item: EditableInspectionItem,
    editable: Boolean,
    onMeasuredChange: (String) -> Unit,
    onPassChange: (Boolean) -> Unit,
    onSeverityChange: (String) -> Unit,
    onDefectChange: (String) -> Unit,
) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(item.itemName, style = MaterialTheme.typography.titleSmall)
            Text("标准：${item.standard.ifBlank { "-" }}")
            if (editable) {
                OutlinedTextField(
                    value = item.measuredValue,
                    onValueChange = onMeasuredChange,
                    label = { Text("实测值") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = item.passed,
                        onClick = { onPassChange(true) },
                        label = { Text("合格") },
                    )
                    FilterChip(
                        selected = !item.passed,
                        onClick = { onPassChange(false) },
                        label = { Text("不合格") },
                    )
                }
                if (!item.passed) {
                    val severities = listOf("INFO", "WARN", "ERROR", "CRITICAL")
                    Text("严重度", style = MaterialTheme.typography.labelSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        severities.forEach { sev ->
                            FilterChip(
                                selected = item.severity == sev,
                                onClick = { onSeverityChange(sev) },
                                label = { Text(sev) },
                            )
                        }
                    }
                    OutlinedTextField(
                        value = item.defectDesc,
                        onValueChange = onDefectChange,
                        label = { Text("不良描述") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            } else {
                Text("实测：${item.measuredValue.ifBlank { "-" }}")
                Text(
                    if (item.passed) "合格" else "不合格 · ${item.severity}",
                    color = if (item.passed) ErpColors.SuccessGreen else ErpColors.ErrorRed,
                )
                if (item.defectDesc.isNotBlank()) Text("不良：${item.defectDesc}")
            }
        }
    }
}
