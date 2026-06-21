package com.btsheng.erp.feature.operator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.btsheng.erp.core.auth.RoleAccess

/**
 * 操作工 APP 工作台说明（依据 docs/CNC_ERP_全链路流程.md · PRD V1.3.8）
 *
 * 展示职责：扫码开工 / 报工 / 过站（GD- / LZ- / SB- 三码）
 */
@Composable
fun OperatorWorkbenchGuide(roles: List<String>, modifier: Modifier = Modifier) {
    val isOperator = RoleAccess.hasAny(roles, listOf("OPERATOR"))
    val title = when {
        isOperator -> "操作工工作台"
        RoleAccess.hasAny(roles, listOf("QC")) -> "品检 · 扫码辅助"
        RoleAccess.hasAny(roles, listOf("PROD_MGR", "PRODUCTION_MANAGER")) -> "生管 · 现场扫码"
        else -> "扫码工作台"
    }
    val goal = when {
        isOperator -> "扫码开工 → 报工录数 → 流转过站"
        RoleAccess.hasAny(roles, listOf("QC")) -> "现场扫码核对工单与工序"
        else -> "查看工单进度 / 辅助报工"
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(
            Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text("职责：$goal", style = MaterialTheme.typography.bodySmall)
            Text("O.1  扫工单码（GD-）开工", style = MaterialTheme.typography.bodyMedium)
            Text("O.2  报工：录投入 / 合格 / 报废数量", style = MaterialTheme.typography.bodyMedium)
            Text("O.3  扫流转码（LZ-）过站 · 可选扫设备码（SB-）", style = MaterialTheme.typography.bodyMedium)
            if (isOperator) {
                Text(
                    "提示：开工后可点「查看图纸」预览当前工序关联图纸（灰度功能）",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                )
            }
        }
    }
}
