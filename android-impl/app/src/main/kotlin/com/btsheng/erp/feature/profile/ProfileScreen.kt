package com.btsheng.erp.feature.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.btsheng.erp.core.auth.AppRoleSpec
import com.btsheng.erp.core.auth.RoleAccess
import com.btsheng.erp.core.update.AppVersionChecker
import com.btsheng.erp.feature.operator.RoleGuideCard
import com.btsheng.erp.ui.theme.ErpColors

enum class ProfileSubPage { HUB, DETAIL, SETTINGS, PAYSLIP, PERFORMANCE }

/** 我的 Tab · 入口 hub（front-end-spec §5.4 Tab4） */
@Composable
fun ProfileScreen(
    pendingCount: Int = 0,
    roles: List<String> = emptyList(),
    realName: String? = null,
    username: String? = null,
    subPage: ProfileSubPage = ProfileSubPage.HUB,
    onNavigate: (ProfileSubPage) -> Unit = {},
    onSyncNow: () -> Unit = {},
    onClearCache: () -> Unit = {},
    onLogout: () -> Unit = {},
    onOpenBatchIncoming: () -> Unit = {},
    onOpenMaterialBarcode: () -> Unit = {},
    onOpenOutsourceArrival: () -> Unit = {},
    onOpenPayslip: () -> Unit = {},
    onOpenPerformance: () -> Unit = {},
    profileDetail: @Composable () -> Unit = {},
    payslipContent: @Composable () -> Unit = {},
    performanceContent: @Composable () -> Unit = {},
    settingsContent: @Composable () -> Unit = {},
) {
    when (subPage) {
        ProfileSubPage.DETAIL -> profileDetail()
        ProfileSubPage.SETTINGS -> settingsContent()
        ProfileSubPage.PAYSLIP -> payslipContent()
        ProfileSubPage.PERFORMANCE -> performanceContent()
        ProfileSubPage.HUB -> ProfileHubScreen(
            pendingCount = pendingCount,
            roles = roles,
            realName = realName,
            username = username,
            onOpenProfile = { onNavigate(ProfileSubPage.DETAIL) },
            onOpenSettings = { onNavigate(ProfileSubPage.SETTINGS) },
            onSyncNow = onSyncNow,
            onClearCache = onClearCache,
            onLogout = onLogout,
            onOpenBatchIncoming = onOpenBatchIncoming,
            onOpenMaterialBarcode = onOpenMaterialBarcode,
            onOpenOutsourceArrival = onOpenOutsourceArrival,
            onOpenPayslip = onOpenPayslip,
            onOpenPerformance = onOpenPerformance,
        )
    }
}

@Composable
private fun ProfileHubScreen(
    pendingCount: Int,
    roles: List<String>,
    realName: String?,
    username: String?,
    onOpenProfile: () -> Unit,
    onOpenSettings: () -> Unit,
    onSyncNow: () -> Unit,
    onClearCache: () -> Unit,
    onLogout: () -> Unit,
    onOpenBatchIncoming: () -> Unit,
    onOpenMaterialBarcode: () -> Unit,
    onOpenOutsourceArrival: () -> Unit,
    onOpenPayslip: () -> Unit,
    onOpenPerformance: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        ) {
            Row(
                Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Surface(
                    modifier = Modifier.size(56.dp).clip(CircleShape),
                    color = ErpColors.IndustrialBlue,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("BTS", color = ErpColors.White, fontWeight = FontWeight.Bold)
                    }
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        realName?.takeIf { it.isNotBlank() } ?: username ?: "用户",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (!username.isNullOrBlank()) Text("@$username", style = MaterialTheme.typography.bodySmall)
                    if (roles.isNotEmpty()) {
                        Text(roles.joinToString(" · "), style = MaterialTheme.typography.labelMedium, color = ErpColors.TextSecondary)
                    }
                }
            }
        }

        Text("账户", style = MaterialTheme.typography.titleSmall, color = ErpColors.TextSecondary)
        ProfileMenuItem("个人资料", "查看姓名、手机、角色", Icons.Filled.Person, onOpenProfile)
        ProfileMenuItem("我的工资", "近 12 个月工资条", Icons.Filled.Payments, onOpenPayslip)
        ProfileMenuItem("我的绩效", "绩效分数与申诉", Icons.Filled.Star, onOpenPerformance)
        ProfileMenuItem("设置", "账号安全、通知、版本更新", Icons.Filled.Settings, onOpenSettings)

        Text("离线与同步", style = MaterialTheme.typography.titleSmall, color = ErpColors.TextSecondary)
        ListItem(
            headlineContent = { Text("离线缓存") },
            supportingContent = { Text("待同步 $pendingCount 条（上限 500）") },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(onClick = onSyncNow, modifier = Modifier.weight(1f)) { Text("立即同步") }
            OutlinedButton(onClick = onClearCache, modifier = Modifier.weight(1f)) { Text("清空队列") }
        }

        if (RoleAccess.canUseWarehouseTools(roles)) {
            AppRoleSpec.warehouseToolSections(roles)?.let { section ->
                Text(section.title, style = MaterialTheme.typography.titleSmall, color = ErpColors.TextSecondary)
                section.items.forEach { tool ->
                    val onClick = when (tool.kind) {
                        AppRoleSpec.ProfileToolKind.BATCH_INCOMING -> onOpenBatchIncoming
                        AppRoleSpec.ProfileToolKind.OUTSOURCE_ARRIVAL -> onOpenOutsourceArrival
                        AppRoleSpec.ProfileToolKind.MATERIAL_BARCODE -> onOpenMaterialBarcode
                    }
                    ProfileMenuItem(tool.title, tool.subtitle, iconForTool(tool.kind), onClick)
                }
            }
        } else if (AppRoleSpec.profileGuideTitle(roles) != null) {
            RoleGuideCard(roles = roles)
        }

        Text(
            AppVersionChecker.currentVersionLabel(),
            style = MaterialTheme.typography.labelSmall,
            color = ErpColors.TextSecondary,
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp),
        )

        OutlinedButton(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = ErpColors.ErrorRed),
        ) { Text("退出登录") }
    }
}

@Composable
private fun iconForTool(kind: AppRoleSpec.ProfileToolKind) = when (kind) {
    AppRoleSpec.ProfileToolKind.BATCH_INCOMING -> Icons.Filled.Inventory
    AppRoleSpec.ProfileToolKind.OUTSOURCE_ARRIVAL -> Icons.Filled.LocalShipping
    AppRoleSpec.ProfileToolKind.MATERIAL_BARCODE -> Icons.Filled.QrCodeScanner
}

@Composable
private fun ProfileMenuItem(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        ListItem(
            headlineContent = { Text(title) },
            supportingContent = { Text(subtitle, style = MaterialTheme.typography.bodySmall) },
            leadingContent = { Icon(icon, contentDescription = null, tint = ErpColors.IndustrialBlue) },
            trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
        )
    }
}
