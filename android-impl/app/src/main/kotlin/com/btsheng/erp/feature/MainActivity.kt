package com.btsheng.erp.feature

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import com.btsheng.erp.R
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.btsheng.erp.core.auth.AppRoleSpec
import com.btsheng.erp.core.auth.RoleAccess
import com.btsheng.erp.core.auth.RoleResolver
import com.btsheng.erp.core.network.LoginRequest
import com.btsheng.erp.core.security.SecureSessionManager
import com.btsheng.erp.core.security.SessionStore
import com.btsheng.erp.feature.auth.LoginScreen
import com.btsheng.erp.feature.auth.LoginViewModel
import com.btsheng.erp.feature.message.MessageListScreen
import com.btsheng.erp.feature.message.MessageListViewModel
import com.btsheng.erp.feature.profile.ProfileScreen
import com.btsheng.erp.feature.hr.PayslipScreen
import com.btsheng.erp.feature.hr.PerformanceAppealScreen
import com.btsheng.erp.feature.profile.ProfileDetailScreen
import com.btsheng.erp.feature.profile.ProfileSubPage
import com.btsheng.erp.feature.profile.ProfileViewModel
import com.btsheng.erp.feature.profile.SettingsScreen
import com.btsheng.erp.feature.scan.ScanScreen
import com.btsheng.erp.feature.scan.ScanViewModel
import com.btsheng.erp.feature.qc.ConcessionApprovalActivity
import com.btsheng.erp.feature.qc.InspectionDetailActivity
import com.btsheng.erp.feature.todo.TodoAction
import com.btsheng.erp.feature.todo.TodoItem
import com.btsheng.erp.feature.todo.TodoScreen
import com.btsheng.erp.feature.todo.TodoViewModel
import com.btsheng.erp.feature.v138.BatchIncomingScanActivity
import com.btsheng.erp.feature.v138.MaterialBarcodeScanActivity
import com.btsheng.erp.feature.v138.MaterialScanMode
import com.btsheng.erp.feature.v138.OutsourceArrivalScanActivity
import com.btsheng.erp.feature.v138.OutsourceArrivalScanViewModel
import com.btsheng.erp.feature.warehouse.WarehouseScreen
import com.btsheng.erp.core.update.AppVersionChecker
import com.btsheng.erp.ui.AppTopBar
import com.btsheng.erp.ui.NetworkStatusStrip
import com.btsheng.erp.ui.theme.ErpTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_Erp)
        super.onCreate(savedInstanceState)
        setContent {
            ErpTheme {
                ErpAppRoot()
            }
        }
    }
}

@Composable
private fun ErpAppRoot() {
    var authState by remember { mutableStateOf(AuthState.CHECKING) }

    LaunchedEffect(Unit) {
        authState = if (SecureSessionManager.isTokenValid() && SecureSessionManager.restoreSession()) {
            AuthState.LOGGED_IN
        } else {
            AuthState.LOGGED_OUT
        }
    }

    when (authState) {
        AuthState.CHECKING -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        AuthState.LOGGED_OUT -> {
            LoginGate(onLoggedIn = { authState = AuthState.LOGGED_IN })
        }
        AuthState.LOGGED_IN -> {
            ErpMainScaffold(onLogout = {
                SessionStore.logout()
                authState = AuthState.LOGGED_OUT
            })
        }
    }
}

private enum class AuthState { CHECKING, LOGGED_OUT, LOGGED_IN }

@Composable
private fun LoginGate(
    onLoggedIn: () -> Unit,
    loginViewModel: LoginViewModel = hiltViewModel(),
) {
    val scope = rememberCoroutineScope()
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var rememberPassword by remember { mutableStateOf(SecureSessionManager.isRememberPassword()) }

    suspend fun doLogin(user: String, pass: String) {
        loading = true
        error = null
        try {
            val resp = loginViewModel.authApi.login(LoginRequest(user, pass))
            if (!resp.ok || resp.data?.accessToken.isNullOrBlank()) {
                error = resp.message ?: "登录失败，请检查账号密码"
                return
            }
            val d = resp.data!!
            val roles = RoleResolver.resolve(d.roles, d.user?.username ?: user)
            SessionStore.saveFromLogin(
                accessToken = d.accessToken!!,
                refreshToken = d.refreshToken ?: "",
                userId = d.user?.id ?: 1L,
                username = d.user?.username ?: user,
                realName = d.user?.realName,
                roles = roles,
            )
            SecureSessionManager.setRememberPassword(rememberPassword, user, pass)
            onLoggedIn()
        } catch (e: Exception) {
            error = e.message ?: "网络错误，请检查服务器连接"
        } finally {
            loading = false
        }
    }

    LoginScreen(
        loading = loading,
        error = error,
        initialUsername = SecureSessionManager.savedUsername(),
        initialPassword = if (SecureSessionManager.isRememberPassword()) SecureSessionManager.savedPassword() else "",
        rememberPassword = rememberPassword,
        onRememberPasswordChange = { rememberPassword = it },
        onLogin = { user, pass -> scope.launch { doLogin(user, pass) } },
        debugQuickAccounts = listOf(
            "operator" to "操作工",
            "warehouse" to "仓管",
            "qc" to "品检",
            "prod_mgr" to "生管",
            "buyer" to "采购",
            "procurement_manager" to "采购主管",
        ),
        onQuickLogin = { user, pass -> scope.launch { doLogin(user, pass) } },
    )
}

@Composable
private fun ErpMainScaffold(
    onLogout: () -> Unit,
    scanViewModel: ScanViewModel = hiltViewModel(),
    messageViewModel: MessageListViewModel = hiltViewModel(),
    todoViewModel: TodoViewModel = hiltViewModel(),
    profileViewModel: ProfileViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val session = SessionStore.session
    val roles = remember(session?.username, session?.roles) {
        RoleResolver.resolve(session?.roles, session?.username)
    }
    LaunchedEffect(roles, session?.username) {
        session?.let { s ->
            if (s.roles != roles) {
                SessionStore.session = s.copy(roles = roles)
                SecureSessionManager.updateRoles(roles)
            }
        }
    }
    val tabs = remember(roles) { RoleAccess.visibleTabs(roles) }
    var selectedTab by remember(tabs) {
        mutableIntStateOf(tabs.indexOf(RoleAccess.defaultTab(roles)).coerceAtLeast(0))
    }
    var profileSubPage by remember { mutableStateOf(ProfileSubPage.HUB) }
    var approvalHint by remember { mutableStateOf<String?>(null) }
    if (selectedTab >= tabs.size) selectedTab = 0

    val messages by messageViewModel.messages.collectAsState()
    val messageLoading by messageViewModel.loading.collectAsState()
    val todoItems by todoViewModel.items.collectAsState()
    val profile by profileViewModel.profile.collectAsState()
    val profileLoading by profileViewModel.loading.collectAsState()
    val profileError by profileViewModel.error.collectAsState()
    val checkingUpdate by profileViewModel.checkingUpdate.collectAsState()
    val updateInfo by profileViewModel.updateInfo.collectAsState()
    val updateMessage by profileViewModel.updateMessage.collectAsState()

    val messageCount = messages.count { !it.read }.takeIf { it > 0 } ?: messages.size
    val todoCount = todoItems.size

    LaunchedEffect(Unit) {
        profileViewModel.checkForUpdate(manual = false)
    }

    LaunchedEffect(profileSubPage) {
        if (profileSubPage == ProfileSubPage.DETAIL) {
            profileViewModel.loadProfile()
        }
    }

    fun tabIndexOf(tab: RoleAccess.AppTab): Int = tabs.indexOf(tab).takeIf { it >= 0 } ?: 0

    fun handleTodoAction(item: TodoItem) {
        when (item.action) {
            TodoAction.INSPECTION_DETAIL -> item.actionId?.let { id ->
                context.startActivity(InspectionDetailActivity.intent(context, id))
            }
            TodoAction.CONCESSION_APPROVAL -> item.actionId?.let { id ->
                context.startActivity(ConcessionApprovalActivity.detailIntent(context, id))
            } ?: context.startActivity(ConcessionApprovalActivity.listIntent(context))
            TodoAction.OPEN_WAREHOUSE -> selectedTab = tabIndexOf(RoleAccess.AppTab.WAREHOUSE)
            TodoAction.SCAN_WORKORDER, TodoAction.WORKORDER_PROGRESS ->
                selectedTab = tabIndexOf(RoleAccess.AppTab.SCAN)
            TodoAction.APPROVAL_DETAIL -> {
                approvalHint = buildString {
                    append("单据：${item.title}\n")
                    append(item.subtitle)
                    append("\n\n请在 PC 端「审批中心」处理；")
                    append("消息中心同步推送 APPROVAL_NOTIFY。")
                    item.actionId?.let { append("\n审批 ID：$it") }
                }
            }
            TodoAction.NONE -> Unit
        }
    }

    approvalHint?.let { hint ->
        AlertDialog(
            onDismissRequest = { approvalHint = null },
            title = { Text("审批待办") },
            text = { Text(hint) },
            confirmButton = {
                TextButton(onClick = { approvalHint = null }) { Text("知道了") }
            },
        )
    }

    updateInfo?.let { info ->
        AppUpdateDialog(
            info = info,
            onDismiss = { if (!info.forceUpdate) profileViewModel.dismissUpdateDialog() },
            onDownload = {
                if (info.downloadUrl.isNotBlank()) {
                    context.startActivity(
                        android.content.Intent(
                            android.content.Intent.ACTION_VIEW,
                            android.net.Uri.parse(info.downloadUrl),
                        ),
                    )
                }
                if (!info.forceUpdate) profileViewModel.dismissUpdateDialog()
            },
        )
    }

    Scaffold(
        topBar = {
            Column {
                AppTopBar(
                    userName = SessionStore.displayName(),
                    roleLabel = AppRoleSpec.roleDisplayName(roles),
                    online = scanViewModel.isOnline,
                    pendingSync = scanViewModel.pendingCount,
                )
                NetworkStatusStrip(
                    online = scanViewModel.isOnline,
                    pendingSync = scanViewModel.pendingCount,
                )
            }
        },
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, tab ->
                    val spec = tabSpec(tab)
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = {
                            selectedTab = index
                            if (tab == RoleAccess.AppTab.PROFILE) {
                                profileSubPage = ProfileSubPage.HUB
                            }
                        },
                        icon = { Icon(spec.icon, contentDescription = spec.title) },
                        label = { Text(spec.title) },
                    )
                }
            }
        },
    ) { innerPadding ->
        Box(Modifier.padding(innerPadding)) {
            when (tabs.getOrNull(selectedTab)) {
                RoleAccess.AppTab.SCAN -> ScanScreen(
                    roles = roles,
                    viewModel = scanViewModel,
                    todoCount = todoCount,
                    messageCount = messageCount,
                    onOpenTodo = { selectedTab = tabIndexOf(RoleAccess.AppTab.TODO) },
                    onOpenMessage = { selectedTab = tabIndexOf(RoleAccess.AppTab.MESSAGE) },
                    onOpenSettings = { selectedTab = tabIndexOf(RoleAccess.AppTab.PROFILE) },
                    onOpenMenu = { selectedTab = tabIndexOf(RoleAccess.AppTab.PROFILE) },
                )
                RoleAccess.AppTab.WAREHOUSE -> WarehouseScreen(
                    roles = roles,
                    onScanInbound = { code ->
                        context.startActivity(
                            MaterialBarcodeScanActivity.intent(context, MaterialScanMode.INBOUND, code.takeIf { it.isNotBlank() }),
                        )
                    },
                    onScanOutbound = { code ->
                        context.startActivity(
                            MaterialBarcodeScanActivity.intent(context, MaterialScanMode.OUTBOUND, code.takeIf { it.isNotBlank() }),
                        )
                    },
                    onScanArrival = { code ->
                        val trimmed = code.trim()
                        when {
                            OutsourceArrivalScanViewModel.isOutsourceCode(trimmed) -> {
                                context.startActivity(
                                    OutsourceArrivalScanActivity.intent(
                                        context,
                                        OutsourceArrivalScanViewModel.normalizeOutsourceNo(trimmed),
                                    ),
                                )
                            }
                            trimmed.toLongOrNull() != null -> {
                                context.startActivity(BatchIncomingScanActivity.intent(context, trimmed.toLong()))
                            }
                            else -> {
                                context.startActivity(OutsourceArrivalScanActivity.intent(context))
                            }
                        }
                    },
                    onBatchIncoming = {
                        context.startActivity(BatchIncomingScanActivity.intent(context, 1001L))
                    },
                )
                RoleAccess.AppTab.TODO -> TodoScreen(
                    viewModel = todoViewModel,
                    onItemAction = { handleTodoAction(it) },
                )
                RoleAccess.AppTab.MESSAGE -> MessageListScreen(
                    messages = messages,
                    loading = messageLoading,
                    roles = roles,
                    onRefresh = { messageViewModel.reload() },
                    onMessageClick = { msg ->
                        messageViewModel.markRead(msg.id)
                        if (msg.type == "APPROVAL_NOTIFY") {
                            approvalHint = buildString {
                                append(msg.title).append('\n').append(msg.content)
                                if (msg.routeUrl.isNotBlank()) append("\n\nPC 路由：").append(msg.routeUrl)
                                append("\n\n审批操作请在 PC 端完成，APP 仅推送通知。")
                            }
                        }
                    },
                )
                RoleAccess.AppTab.PROFILE -> ProfileScreen(
                    pendingCount = scanViewModel.pendingCount,
                    roles = roles,
                    realName = SessionStore.session?.realName,
                    username = SessionStore.session?.username,
                    subPage = profileSubPage,
                    onNavigate = { profileSubPage = it },
                    onSyncNow = { scanViewModel.refreshPending() },
                    onClearCache = { scanViewModel.refreshPending() },
                    onLogout = onLogout,
                    onOpenBatchIncoming = {
                        context.startActivity(BatchIncomingScanActivity.intent(context, 1001L))
                    },
                    onOpenOutsourceArrival = {
                        context.startActivity(
                            OutsourceArrivalScanActivity.intent(context, "WW20260612-0001"),
                        )
                    },
                    onOpenMaterialBarcode = {
                        context.startActivity(MaterialBarcodeScanActivity.intent(context, MaterialScanMode.INBOUND))
                    },
                    onOpenPayslip = { profileSubPage = ProfileSubPage.PAYSLIP },
                    onOpenPerformance = { profileSubPage = ProfileSubPage.PERFORMANCE },
                    profileDetail = {
                        ProfileDetailScreen(
                            profile = profile,
                            loading = profileLoading,
                            error = profileError,
                            onBack = { profileSubPage = ProfileSubPage.HUB },
                            onRefresh = { profileViewModel.loadProfile() },
                        )
                    },
                    payslipContent = {
                        PayslipScreen(onBack = { profileSubPage = ProfileSubPage.HUB })
                    },
                    performanceContent = {
                        PerformanceAppealScreen(onBack = { profileSubPage = ProfileSubPage.HUB })
                    },
                    settingsContent = {
                        SettingsScreen(
                            checkingUpdate = checkingUpdate,
                            updateInfo = updateInfo,
                            updateMessage = updateMessage,
                            onBack = { profileSubPage = ProfileSubPage.HUB },
                            onCheckUpdate = { profileViewModel.checkForUpdate(manual = true) },
                            onDismissUpdate = { profileViewModel.dismissUpdateDialog() },
                        )
                    },
                )
                else -> ProfileScreen(onLogout = onLogout)
            }
        }
    }
}

private data class TabUiSpec(
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
)

private fun tabSpec(tab: RoleAccess.AppTab) = when (tab) {
    RoleAccess.AppTab.SCAN -> TabUiSpec("扫码", Icons.Filled.Home)
    RoleAccess.AppTab.WAREHOUSE -> TabUiSpec("仓储", Icons.Filled.Inventory)
    RoleAccess.AppTab.TODO -> TabUiSpec("待办", Icons.Filled.Assignment)
    RoleAccess.AppTab.MESSAGE -> TabUiSpec("消息", Icons.Filled.Mail)
    RoleAccess.AppTab.PROFILE -> TabUiSpec("我的", Icons.Filled.Person)
}

@Composable
private fun AppUpdateDialog(
    info: AppVersionChecker.UpdateInfo,
    onDismiss: () -> Unit,
    onDownload: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("发现新版本 ${info.latestVersionName}") },
        text = {
            Text("当前 ${AppVersionChecker.currentVersionLabel()}\n${if (info.forceUpdate) "请立即更新后继续使用。" else "建议更新到最新版本。"}")
        },
        confirmButton = {
            TextButton(onClick = onDownload) {
                Text(if (info.downloadUrl.isNotBlank()) "下载更新" else "知道了")
            }
        },
        dismissButton = if (!info.forceUpdate) {
            { TextButton(onClick = onDismiss) { Text("稍后") } }
        } else null,
    )
}
