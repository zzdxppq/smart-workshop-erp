package com.btsheng.erp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.btsheng.erp.ui.theme.ErpColors

/**
 * APP 工作台顶栏（ux-handoff §4.1 · front-end-spec §6.3.1）
 *
 * 昆山佰泰胜 ERP · 用户名 · 在线/离线状态
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    userName: String,
    roleLabel: String = "",
    online: Boolean,
    pendingSync: Int = 0,
    modifier: Modifier = Modifier,
    onNetworkMenuClick: () -> Unit = {},
) {
    val statusLabel = when {
        !online -> "离线"
        pendingSync > 0 -> "同步中"
        else -> "在线"
    }
    val statusColor = when {
        !online -> ErpColors.ErrorRed
        pendingSync > 0 -> ErpColors.WarningYellow
        else -> ErpColors.SuccessGreen
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                "昆山佰泰胜 ERP",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = ErpColors.TextPrimary,
            )
            TextButton(onClick = onNetworkMenuClick) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            userName.ifBlank { "未登录" },
                            style = MaterialTheme.typography.bodyMedium,
                            color = ErpColors.TextSecondary,
                        )
                        if (roleLabel.isNotBlank()) {
                            Text(
                                roleLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = ErpColors.IndustrialBlue,
                            )
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    Box(
                        Modifier
                            .size(8.dp)
                            .background(statusColor, shape = MaterialTheme.shapes.small),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(statusLabel, style = MaterialTheme.typography.labelMedium, color = statusColor)
                    Icon(
                        Icons.Filled.KeyboardArrowDown,
                        contentDescription = "网络状态",
                        modifier = Modifier.size(18.dp),
                        tint = ErpColors.TextSecondary,
                    )
                }
            }
        }
    }
}

/** 32px 网络状态条（front-end-spec §6.3.1） */
@Composable
fun NetworkStatusStrip(
    online: Boolean,
    pendingSync: Int,
    modifier: Modifier = Modifier,
) {
    val (bg, text) = when {
        !online -> MaterialTheme.colorScheme.errorContainer to "离线模式 · 待同步 $pendingSync 条"
        pendingSync > 0 -> MaterialTheme.colorScheme.tertiaryContainer to "同步中 · 队列 $pendingSync 条待上传 ↑"
        else -> MaterialTheme.colorScheme.primaryContainer to "在线 · 数据已同步"
    }
    Surface(color = bg, modifier = modifier.fillMaxWidth()) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
        )
    }
}
