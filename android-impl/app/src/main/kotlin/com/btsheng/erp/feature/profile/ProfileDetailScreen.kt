package com.btsheng.erp.feature.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.btsheng.erp.core.network.UserProfileDto
import com.btsheng.erp.ui.theme.ErpColors

/** 个人资料详情（front-end-spec §5.4 · /me/profile） */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileDetailScreen(
    profile: UserProfileDto?,
    loading: Boolean,
    error: String?,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("个人资料") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    TextButton(onClick = onRefresh) { Text("刷新") }
                },
            )
        },
    ) { padding ->
        when {
            loading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            profile == null -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(error ?: "无法加载个人资料", color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = onRefresh) { Text("重试") }
                }
            }
            else -> Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ProfileInfoCard(profile)
            }
        }
    }
}

@Composable
private fun ProfileInfoCard(profile: UserProfileDto) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ProfileField("姓名", profile.realName ?: "—")
            ProfileField("账号", profile.username ?: "—")
            ProfileField("手机", profile.phone ?: "—")
            ProfileField("邮箱", profile.email ?: "—")
            ProfileField("部门 ID", profile.deptId?.toString() ?: "—")
            ProfileField("状态", profile.status ?: "ACTIVE")
            ProfileField("角色", profile.roleCodes?.joinToString(" · ") ?: "—")
        }
    }
}

@Composable
private fun ProfileField(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = ErpColors.TextSecondary)
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
    }
}
