package com.btsheng.erp.feature.message

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.btsheng.erp.core.auth.RoleAccess
import com.btsheng.erp.ui.theme.ErpColors

/** 消息中心（AC-1.4.2 · front-end-spec §5.4 Tab3） */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageListScreen(
    messages: List<MessageViewModel.AppMessage>,
    loading: Boolean,
    roles: List<String> = emptyList(),
    onRefresh: () -> Unit,
    onMessageClick: (MessageViewModel.AppMessage) -> Unit,
) {
    val unread = messages.count { !it.read }
    var selected by remember { mutableStateOf<MessageViewModel.AppMessage?>(null) }

    selected?.let { msg ->
        AlertDialog(
            onDismissRequest = { selected = null },
            icon = { Icon(messageIcon(msg.type), contentDescription = null, tint = messageColor(msg.type)) },
            title = { Text(msg.title) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(msg.content)
                    Text("类型：${messageTypeLabel(msg.type)}", style = MaterialTheme.typography.labelSmall)
                    if (msg.type == "APPROVAL_NOTIFY") {
                        Text(
                            "审批请在 PC 端处理 · APP 同步通知",
                            style = MaterialTheme.typography.bodySmall,
                            color = ErpColors.IndustrialBlue,
                        )
                    }
                    if (msg.timeLabel.isNotBlank()) {
                        Text(msg.timeLabel, style = MaterialTheme.typography.labelSmall, color = ErpColors.TextSecondary)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onMessageClick(msg)
                    selected = null
                }) { Text("标记已读") }
            },
            dismissButton = {
                TextButton(onClick = { selected = null }) { Text("关闭") }
            },
        )
    }

    Column(Modifier.fillMaxSize()) {
        Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 1.dp) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("消息中心", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text(
                        if (unread > 0) "$unread 条未读" else "全部已读",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (unread > 0) ErpColors.ErrorRed else ErpColors.TextSecondary,
                    )
                }
                AssistChip(
                    onClick = onRefresh,
                    label = { Text("刷新") },
                    leadingIcon = { Icon(Icons.Filled.Refresh, contentDescription = null, Modifier.size(16.dp)) },
                )
            }
        }

        Box(Modifier.fillMaxSize()) {
            if (loading && messages.isEmpty()) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            } else if (messages.isEmpty()) {
                EmptyMessageState(hint = RoleAccess.messageEmptyHint(roles))
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(messages, key = { it.id }) { msg ->
                        MessageCard(msg = msg, onClick = { selected = msg })
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageCard(msg: MessageViewModel.AppMessage, onClick: () -> Unit) {
    val color = messageColor(msg.type)
    OutlinedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Surface(
                modifier = Modifier.size(40.dp).clip(CircleShape),
                color = color.copy(alpha = 0.15f),
            ) {
                Icon(
                    messageIcon(msg.type),
                    contentDescription = null,
                    modifier = Modifier.padding(8.dp),
                    tint = color,
                )
            }
            Column(Modifier.weight(1f)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        msg.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (msg.read) FontWeight.Normal else FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (!msg.read) {
                        Box(
                            Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(ErpColors.ErrorRed),
                        )
                    }
                }
                Text(
                    messageTypeLabel(msg.type),
                    style = MaterialTheme.typography.labelSmall,
                    color = color,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    msg.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = ErpColors.TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (msg.timeLabel.isNotBlank()) {
                    Text(msg.timeLabel, style = MaterialTheme.typography.labelSmall, color = ErpColors.TextSecondary)
                }
            }
        }
    }
}

@Composable
private fun EmptyMessageState(hint: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Filled.NotificationsNone, contentDescription = null, modifier = Modifier.size(48.dp), tint = ErpColors.TextSecondary)
            Text("暂无消息", style = MaterialTheme.typography.titleMedium)
            Text(hint, style = MaterialTheme.typography.bodySmall, color = ErpColors.TextSecondary)
        }
    }
}

private fun messageTypeLabel(type: String) = when (type) {
    "APPROVAL_NOTIFY" -> "审批通知"
    "OVERDUE_NOTIFY" -> "逾期提醒"
    "EXCEPTION_REPORT" -> "异常上报"
    "SCAN_RECEIPT" -> "扫码回执"
    else -> type
}

private fun messageIcon(type: String): ImageVector = when (type) {
    "APPROVAL_NOTIFY" -> Icons.Filled.TaskAlt
    "OVERDUE_NOTIFY" -> Icons.Filled.Schedule
    "EXCEPTION_REPORT" -> Icons.Filled.Warning
    "SCAN_RECEIPT" -> Icons.Filled.QrCodeScanner
    else -> Icons.Filled.Notifications
}

private fun messageColor(type: String) = when (type) {
    "APPROVAL_NOTIFY" -> ErpColors.IndustrialBlue
    "OVERDUE_NOTIFY" -> ErpColors.WarningYellow
    "EXCEPTION_REPORT" -> ErpColors.ErrorRed
    "SCAN_RECEIPT" -> ErpColors.SuccessGreen
    else -> ErpColors.TextSecondary
}
