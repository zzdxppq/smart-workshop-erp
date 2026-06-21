package com.btsheng.erp.feature.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.btsheng.erp.BuildConfig
import com.btsheng.erp.core.security.SecureSessionManager
import com.btsheng.erp.core.update.AppVersionChecker
import com.btsheng.erp.ui.theme.ErpColors

/** 设置页（front-end-spec §5.4 · /me/settings） */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    checkingUpdate: Boolean,
    updateInfo: AppVersionChecker.UpdateInfo?,
    updateMessage: String?,
    onBack: () -> Unit,
    onCheckUpdate: () -> Unit,
    onDismissUpdate: () -> Unit,
) {
    var rememberPassword by remember { mutableStateOf(SecureSessionManager.isRememberPassword()) }
    var pushEnabled by remember { mutableStateOf(SecureSessionManager.isPushEnabled()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("账号与安全", style = MaterialTheme.typography.titleSmall, color = ErpColors.TextSecondary)
            SettingsSwitch(
                title = "记住密码",
                subtitle = "下次启动自动填充账号",
                checked = rememberPassword,
                onCheckedChange = {
                    rememberPassword = it
                    SecureSessionManager.setRememberPassword(
                        it,
                        SecureSessionManager.savedUsername(),
                        if (it) SecureSessionManager.savedPassword() else "",
                    )
                },
            )

            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            Text("通知", style = MaterialTheme.typography.titleSmall, color = ErpColors.TextSecondary)
            SettingsSwitch(
                title = "消息推送",
                subtitle = "审批 / 逾期 / 扫码回执",
                checked = pushEnabled,
                onCheckedChange = {
                    pushEnabled = it
                    SecureSessionManager.setPushEnabled(it)
                },
            )

            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            Text("关于", style = MaterialTheme.typography.titleSmall, color = ErpColors.TextSecondary)
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("昆山佰泰胜 ERP", fontWeight = FontWeight.SemiBold)
                    Text("当前版本：${AppVersionChecker.currentVersionLabel()}", style = MaterialTheme.typography.bodyMedium)
                    Text("合同：${BuildConfig.CONTRACT_ID}", style = MaterialTheme.typography.bodySmall, color = ErpColors.TextSecondary)
                    Text("技术支持：${BuildConfig.VENDOR}", style = MaterialTheme.typography.bodySmall, color = ErpColors.TextSecondary)
                    if (!updateMessage.isNullOrBlank()) {
                        Text(updateMessage, style = MaterialTheme.typography.bodySmall, color = ErpColors.SuccessGreen)
                    }
                    Button(
                        onClick = onCheckUpdate,
                        enabled = !checkingUpdate,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(if (checkingUpdate) "检查中…" else "检查新版本")
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSwitch(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle, style = MaterialTheme.typography.bodySmall) },
        trailingContent = {
            Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
        },
    )
}
