package com.btsheng.erp.feature.warehouse

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.btsheng.erp.core.auth.AppRoleSpec
import com.btsheng.erp.core.scan.BarcodeScanHelper
import com.journeyapps.barcodescanner.ScanContract

/**
 * 仓储 Tab · 入库 / 出库 / 到货（PRD E4-S2 + E12-S2）
 *
 * 支持 **摄像头扫码 + 手动输入**（与 ScanScreen 一致）
 */
@Composable
fun WarehouseScreen(
    roles: List<String> = emptyList(),
    onScanInbound: (String) -> Unit = {},
    onScanOutbound: (String) -> Unit = {},
    onScanArrival: (String) -> Unit = {},
    onBatchIncoming: () -> Unit = {},
) {
    var mode by remember { mutableStateOf("inbound") }
    var code by remember { mutableStateOf("") }

    val scanPrompt = when (mode) {
        "inbound" -> "扫 WL- 物料码 · 入库"
        "outbound" -> "扫 WL- 物料码 · 出库"
        else -> "扫 WW- 委外单码 或输入 PO ID"
    }

    val scanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        result.contents?.trim()?.takeIf { it.isNotEmpty() }?.let { scanned ->
            code = scanned
            dispatchWarehouseAction(mode, scanned, onScanInbound, onScanOutbound, onScanArrival)
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("仓储扫码 · ${AppRoleSpec.roleDisplayName(roles)}", style = MaterialTheme.typography.headlineSmall)
        Text(
            "入库/出库扫 WL- · 到货扫 WW- 或 PO（ux-handoff §4.7）",
            style = MaterialTheme.typography.bodySmall,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = mode == "inbound", onClick = { mode = "inbound" }, label = { Text("入库 WL-") })
            FilterChip(selected = mode == "outbound", onClick = { mode = "outbound" }, label = { Text("出库 WL-") })
            FilterChip(selected = mode == "arrival", onClick = { mode = "arrival" }, label = { Text("到货 WW-/PO") })
        }

        // 扫码主区（PRD：扫码即一切）
        OutlinedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { scanLauncher.launch(BarcodeScanHelper.options(scanPrompt)) },
                    modifier = Modifier.fillMaxWidth().testTag("warehouse_scan_camera"),
                ) { Text("打开摄像头扫码") }
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = {
                        Text(
                            when (mode) {
                                "inbound", "outbound" -> "或手动输入物料码 WL-"
                                else -> "或手动输入 WW- / PO ID"
                            },
                        )
                    },
                    modifier = Modifier.fillMaxWidth().testTag("warehouse_manual_input"),
                    singleLine = true,
                )
            }
        }

        Button(
            onClick = {
                val trimmed = code.trim()
                if (trimmed.isEmpty()) return@Button
                dispatchWarehouseAction(mode, trimmed, onScanInbound, onScanOutbound, onScanArrival)
            },
            modifier = Modifier.fillMaxWidth().testTag("warehouse_confirm"),
            enabled = code.trim().isNotEmpty(),
        ) {
            Text(
                when (mode) {
                    "inbound" -> "确认入库"
                    "outbound" -> "确认出库"
                    else -> "到货登记"
                },
            )
        }
        OutlinedButton(onClick = onBatchIncoming, modifier = Modifier.fillMaxWidth()) {
            Text("PO 分批到货（演示 PO 1001）")
        }
    }
}

private fun dispatchWarehouseAction(
    mode: String,
    value: String,
    onScanInbound: (String) -> Unit,
    onScanOutbound: (String) -> Unit,
    onScanArrival: (String) -> Unit,
) {
    when (mode) {
        "inbound" -> onScanInbound(value)
        "outbound" -> onScanOutbound(value)
        else -> onScanArrival(value)
    }
}
