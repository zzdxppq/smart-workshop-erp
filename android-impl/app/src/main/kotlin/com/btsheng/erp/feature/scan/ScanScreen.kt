package com.btsheng.erp.feature.scan

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.btsheng.erp.core.auth.AppRoleSpec
import com.btsheng.erp.core.scan.QrCodeParser
import com.btsheng.erp.feature.qc.InspectionDetailActivity
import com.btsheng.erp.feature.v139.WorkorderProcessScanActivity
import com.btsheng.erp.ui.theme.ErpColors
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

/**
 * 工作台首页 · 扫码三码（ux-handoff §4.1 · front-end-spec §8.2）
 *
 * 80% 扫码区 + 码类型提示 + 待办/消息快捷入口
 */
@Composable
fun ScanScreen(
    viewModel: ScanViewModel = hiltViewModel(),
    roles: List<String> = emptyList(),
    todoCount: Int = 0,
    messageCount: Int = 0,
    onOpenTodo: () -> Unit = {},
    onOpenMessage: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenMenu: () -> Unit = {},
) {
    var step by remember { mutableIntStateOf(0) }
    var code by remember { mutableStateOf("") }
    var gdCode by remember { mutableStateOf("") }
    var qtyOk by remember { mutableStateOf("1") }
    var qtyScrap by remember { mutableStateOf("0") }
    var qtyDone by remember { mutableStateOf("1") }
    var showAdvanced by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val primaryRole = AppRoleSpec.primaryRole(roles)
    val isOperatorFlow = primaryRole == AppRoleSpec.PrimaryRole.OPERATOR
    val stepTitles = listOf("扫 GD- 开工", "扫 LZ- 过站 / 报工", "扫 SB- 选机台")
    val scanHints = AppRoleSpec.scanHints(roles)
    val scanTitle = AppRoleSpec.scanTabTitle(roles)

    fun processScanResult(scanned: String) {
        val trimmed = scanned.trim()
        if (trimmed.isEmpty()) return
        when (primaryRole) {
            AppRoleSpec.PrimaryRole.OPERATOR -> {
                code = trimmed
                showAdvanced = true
                val parsed = QrCodeParser.parse(trimmed)
                if (parsed.type == QrCodeParser.TYPE_WORK_ORDER && step == 0 && gdCode.isEmpty()) {
                    viewModel.submitStep(0, trimmed, trimmed, 0, 0, 0) { ok ->
                        if (ok) {
                            gdCode = trimmed
                            step = 1
                            code = ""
                        }
                    }
                }
            }
            AppRoleSpec.PrimaryRole.QC, AppRoleSpec.PrimaryRole.PROD_MGR -> {
                viewModel.handleRoleScan(trimmed, primaryRole) { inspectionId ->
                    context.startActivity(InspectionDetailActivity.intent(context, inspectionId))
                }
            }
            else -> code = trimmed
        }
    }

    val scanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        result.contents?.let { processScanResult(it) }
    }

    when (val rs = viewModel.roleScanState) {
        is RoleScanUiState.Loading -> {
            AlertDialog(
                onDismissRequest = {},
                title = { Text("查询中") },
                text = { CircularProgressIndicator() },
                confirmButton = {},
            )
        }
        is RoleScanUiState.Message -> {
            AlertDialog(
                onDismissRequest = { viewModel.clearRoleScanState() },
                title = { Text("扫码结果") },
                text = { Text(rs.text) },
                confirmButton = {
                    TextButton(onClick = { viewModel.clearRoleScanState() }) { Text("确定") }
                },
            )
        }
        is RoleScanUiState.Progress -> {
            val p = rs.info
            AlertDialog(
                onDismissRequest = { viewModel.clearRoleScanState() },
                title = { Text(if (p.found) "工单进度" else "未找到工单") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("工单：${p.workorderNo}")
                        if (p.found) {
                            p.productName?.let { Text("产品：$it") }
                            Text("状态：${p.status ?: "-"} · 进度 ${p.progressPercent ?: 0}%")
                            Text("计划 ${p.qtyPlanned ?: 0} / 完成 ${p.qtyCompleted ?: 0}")
                            p.alertMessage?.let { Text("预警：$it", color = ErpColors.WarningYellow) }
                        } else {
                            Text(p.alertMessage ?: "请核对 GD- 工单码")
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.clearRoleScanState() }) { Text("关闭") }
                },
            )
        }
        RoleScanUiState.Idle -> Unit
    }

    Column(Modifier.fillMaxSize()) {
        if (isOperatorFlow && (showAdvanced || step > 0 || gdCode.isNotEmpty())) {
            ThreeCodeFlowPanel(
                step = step,
                stepTitles = stepTitles,
                code = code,
                onCodeChange = { code = it },
                gdCode = gdCode,
                qtyDone = qtyDone,
                qtyOk = qtyOk,
                qtyScrap = qtyScrap,
                onQtyDoneChange = { qtyDone = it },
                onQtyOkChange = { qtyOk = it },
                onQtyScrapChange = { qtyScrap = it },
                lastMessage = viewModel.lastMessage,
                onScanClick = {
                    scanLauncher.launch(
                        ScanOptions().apply {
                            setDesiredBarcodeFormats(ScanOptions.ALL_CODE_TYPES)
                            setPrompt(stepTitles[step])
                            setBeepEnabled(true)
                            setOrientationLocked(false)
                        },
                    )
                },
                onBackToHome = {
                    showAdvanced = false
                    step = 0
                    gdCode = ""
                    code = ""
                },
                onPrevStep = { if (step > 0) step-- },
                onNextStep = {
                    val trimmed = code.trim()
                    if (trimmed.isEmpty()) return@ThreeCodeFlowPanel
                    val parsed = QrCodeParser.parse(trimmed)
                    when (step) {
                        0 -> if (parsed.type == QrCodeParser.TYPE_WORK_ORDER) {
                            viewModel.submitStep(0, trimmed, trimmed, 0, 0, 0) { ok ->
                                if (ok) {
                                    gdCode = trimmed
                                    step = 1
                                    code = ""
                                }
                            }
                        }
                        1 -> if (parsed.type == QrCodeParser.TYPE_FLOW) {
                            val done = qtyDone.toIntOrNull() ?: 1
                            val ok = qtyOk.toIntOrNull() ?: 1
                            val scrap = qtyScrap.toIntOrNull() ?: 0
                            viewModel.submitStep(1, trimmed, gdCode, done, ok, scrap) { success ->
                                if (success) {
                                    step = 2
                                    code = ""
                                }
                            }
                        }
                        else -> if (parsed.type == QrCodeParser.TYPE_DEVICE || trimmed.isNotEmpty()) {
                            val done = qtyDone.toIntOrNull() ?: 1
                            val ok = qtyOk.toIntOrNull() ?: 1
                            val scrap = qtyScrap.toIntOrNull() ?: 0
                            viewModel.submitStep(2, trimmed, gdCode, done, ok, scrap) {
                                step = 0
                                gdCode = ""
                                code = ""
                                showAdvanced = false
                            }
                        }
                    }
                },
                onOpenDrawing = {
                    if (viewModel.drawingGrayEnabled) {
                        WorkorderProcessScanActivity.launch(context, gdCode)
                    }
                },
                showDrawingEntry = viewModel.drawingGrayEnabled,
            )
        } else {
            WorkbenchHome(
                scanTitle = scanTitle,
                pendingSync = viewModel.pendingCount,
                online = viewModel.isOnline,
                todoCount = todoCount,
                messageCount = messageCount,
                scanHints = scanHints,
                onScanClick = {
                    scanLauncher.launch(
                        ScanOptions().apply {
                            setDesiredBarcodeFormats(ScanOptions.ALL_CODE_TYPES)
                            setPrompt(
                                when (primaryRole) {
                                    AppRoleSpec.PrimaryRole.QC -> "扫 GD-/WL- 核对"
                                    AppRoleSpec.PrimaryRole.PROD_MGR -> "扫 GD- 查进度"
                                    else -> "对准二维码"
                                },
                            )
                            setBeepEnabled(true)
                            setOrientationLocked(false)
                        },
                    )
                },
                onManualInput = {
                    if (isOperatorFlow) showAdvanced = true
                },
                showManualInput = isOperatorFlow,
                onOpenTodo = onOpenTodo,
                onOpenMessage = onOpenMessage,
                onOpenSettings = onOpenSettings,
                onOpenMenu = onOpenMenu,
            )
        }
    }
}

/** ux-handoff §4.1 工作台首页（80% 扫码框） */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkbenchHome(
    scanTitle: String,
    pendingSync: Int,
    online: Boolean,
    todoCount: Int,
    messageCount: Int,
    scanHints: List<String>,
    onScanClick: () -> Unit,
    onManualInput: () -> Unit,
    onOpenTodo: () -> Unit,
    onOpenMessage: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenMenu: () -> Unit,
    showManualInput: Boolean = true,
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .weight(0.8f),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .fillMaxHeight(0.95f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(2.dp, ErpColors.IndustrialBlue),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 4.dp,
            ) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        Icons.Filled.CameraAlt,
                        contentDescription = "扫码",
                        modifier = Modifier.size(64.dp),
                        tint = ErpColors.ManufacturingOrange,
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        scanTitle,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Medium,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "对准二维码扫码",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ErpColors.TextSecondary,
                    )
                    Spacer(Modifier.height(8.dp))
                    scanHints.forEach { hint ->
                        Text(
                            hint,
                            style = MaterialTheme.typography.bodyMedium,
                            color = ErpColors.TextSecondary,
                            textAlign = TextAlign.Center,
                        )
                    }
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = onScanClick,
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .height(56.dp)
                            .testTag("scan_camera_button"),
                        shape = RoundedCornerShape(28.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ErpColors.ManufacturingOrange),
                    ) {
                        Text("打开摄像头", style = MaterialTheme.typography.titleMedium)
                    }
                    TextButton(onClick = onManualInput, enabled = showManualInput) {
                        Text(if (showManualInput) "手动输入 / 三码流程" else "扫码后自动识别")
                    }
                }
            }
        }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            QuickStatChip(
                label = "待办",
                count = todoCount,
                icon = Icons.Filled.TaskAlt,
                onClick = onOpenTodo,
                modifier = Modifier.weight(1f),
            )
            QuickStatChip(
                label = "消息",
                count = messageCount,
                icon = Icons.Filled.Notifications,
                onClick = onOpenMessage,
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(8.dp))

        val offlineText = if (!online) {
            "离线缓存 $pendingSync 条待上传 ↑"
        } else if (pendingSync > 0) {
            "同步队列 $pendingSync 条 ↑"
        } else {
            "数据已同步"
        }
        Text(
            "● $offlineText",
            style = MaterialTheme.typography.bodySmall,
            color = if (!online) ErpColors.ErrorRed else ErpColors.TextSecondary,
            modifier = Modifier.padding(vertical = 4.dp),
        )

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            TextButton(onClick = onOpenSettings) {
                Icon(Icons.Filled.Settings, contentDescription = "设置", modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("设置")
            }
            TextButton(onClick = onOpenMenu) {
                Icon(Icons.Filled.Menu, contentDescription = "菜单", modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("菜单")
            }
        }
    }
}

@Composable
private fun QuickStatChip(
    label: String,
    count: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedCard(onClick = onClick, modifier = modifier) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(icon, contentDescription = label, tint = ErpColors.IndustrialBlue)
            Text("● $label ($count)", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun ThreeCodeFlowPanel(
    step: Int,
    stepTitles: List<String>,
    code: String,
    onCodeChange: (String) -> Unit,
    gdCode: String,
    qtyDone: String,
    qtyOk: String,
    qtyScrap: String,
    onQtyDoneChange: (String) -> Unit,
    onQtyOkChange: (String) -> Unit,
    onQtyScrapChange: (String) -> Unit,
    lastMessage: String?,
    onScanClick: () -> Unit,
    onBackToHome: () -> Unit,
    onPrevStep: () -> Unit,
    onNextStep: () -> Unit,
    onOpenDrawing: () -> Unit,
    showDrawingEntry: Boolean = false,
) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("扫码三码", style = MaterialTheme.typography.headlineSmall)
        LinearProgressIndicator(
            progress = { (step + 1) / 3f },
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        )
        Text(stepTitles[step], style = MaterialTheme.typography.titleMedium)

        Box(
            Modifier
                .fillMaxWidth()
                .weight(0.55f)
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = code,
                    onValueChange = onCodeChange,
                    label = { Text("扫码 / 手动输入") },
                    modifier = Modifier.fillMaxWidth().testTag("scan_manual_input"),
                    singleLine = true,
                )
                Button(onClick = onScanClick, modifier = Modifier.fillMaxWidth()) {
                    Text("打开摄像头扫码")
                }
            }
        }

        if (gdCode.isNotEmpty() && showDrawingEntry) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("工单: $gdCode", color = MaterialTheme.colorScheme.primary)
                TextButton(onClick = onOpenDrawing) { Text("查看图纸") }
            }
        } else if (gdCode.isNotEmpty()) {
            Text("工单: $gdCode", color = MaterialTheme.colorScheme.primary)
        }
        lastMessage?.let { Text(it, style = MaterialTheme.typography.bodySmall) }

        if (step == 1) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(qtyDone, onQtyDoneChange, label = { Text("投入") }, modifier = Modifier.weight(1f))
                OutlinedTextField(qtyOk, onQtyOkChange, label = { Text("合格") }, modifier = Modifier.weight(1f))
                OutlinedTextField(qtyScrap, onQtyScrapChange, label = { Text("报废") }, modifier = Modifier.weight(1f))
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onBackToHome) { Text("返回首页") }
            if (step > 0) {
                OutlinedButton(onClick = onPrevStep) { Text("上一步") }
            }
            Button(
                onClick = onNextStep,
                modifier = Modifier.weight(1f).testTag("scan_next_button"),
            ) {
                Text(if (step < 2) "下一步" else "提交报工")
            }
        }
    }
}

@Composable
fun OfflineStatusBar(pending: Int, online: Boolean) {
    val color = when {
        !online -> MaterialTheme.colorScheme.errorContainer
        pending > 0 -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.primaryContainer
    }
    Surface(color = color, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        Text(
            when {
                !online -> "离线模式 · 待同步 $pending 条"
                pending > 0 -> "同步中 · 队列 $pending 条"
                else -> "在线 · 数据已同步"
            },
            modifier = Modifier.padding(8.dp),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
