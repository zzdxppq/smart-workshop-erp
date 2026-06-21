package com.btsheng.erp.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.btsheng.erp.BuildConfig
import com.btsheng.erp.R
import com.btsheng.erp.ui.theme.ErpColors

/**
 * 登录页（AC-1.4.1 · ux-handoff / front-end-spec · 对齐 Web 佰泰胜工业风）
 */
@Composable
fun LoginScreen(
    loading: Boolean,
    error: String?,
    initialUsername: String,
    initialPassword: String,
    rememberPassword: Boolean,
    onRememberPasswordChange: (Boolean) -> Unit,
    onLogin: (String, String) -> Unit,
    debugQuickAccounts: List<Pair<String, String>> = emptyList(),
    onQuickLogin: ((String, String) -> Unit)? = null,
) {
    var username by remember(initialUsername) { mutableStateOf(initialUsername) }
    var password by remember(initialPassword) { mutableStateOf(initialPassword) }

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(ErpColors.Blue700, ErpColors.IndustrialBlue, ErpColors.BackgroundGray),
                ),
            ),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(24.dp))
            Image(
                painter = painterResource(R.drawable.ic_launcher_foreground),
                contentDescription = "BTS Logo",
                modifier = Modifier.size(96.dp),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "昆山佰泰胜",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = ErpColors.White,
            )
            Text(
                "数字生产 · 数智管理",
                style = MaterialTheme.typography.bodyMedium,
                color = ErpColors.Blue50,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "车间 APP · ${BuildConfig.PRD_VERSION} · ${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.labelMedium,
                color = ErpColors.Blue50.copy(alpha = 0.8f),
            )

            Spacer(Modifier.height(32.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            ) {
                Column(
                    Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        "登录",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        "一码到底，一数到底",
                        style = MaterialTheme.typography.bodySmall,
                        color = ErpColors.TextSecondary,
                    )

                    if (!error.isNullOrBlank()) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Text(
                                error,
                                modifier = Modifier.padding(12.dp),
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }

                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("用户名") },
                        leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                        singleLine = true,
                        enabled = !loading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("username"),
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("密码") },
                        leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { if (!loading) onLogin(username.trim(), password) },
                        ),
                        singleLine = true,
                        enabled = !loading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("password"),
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Checkbox(
                            checked = rememberPassword,
                            onCheckedChange = onRememberPasswordChange,
                            enabled = !loading,
                        )
                        Text("记住密码", style = MaterialTheme.typography.bodyMedium)
                    }

                    Button(
                        onClick = { onLogin(username.trim(), password) },
                        enabled = !loading && username.isNotBlank() && password.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("loginButton"),
                        shape = RoundedCornerShape(24.dp),
                    ) {
                        Text(if (loading) "登录中…" else "登录")
                    }

                    if (BuildConfig.DEBUG && debugQuickAccounts.isNotEmpty() && onQuickLogin != null) {
                        HorizontalDivider()
                        Text(
                            "演示账号（仅 Debug）",
                            style = MaterialTheme.typography.labelMedium,
                            color = ErpColors.TextSecondary,
                        )
                        debugQuickAccounts.forEach { (user, label) ->
                            OutlinedButton(
                                onClick = { onQuickLogin(user, "123456") },
                                enabled = !loading,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(label)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Text(
                "河南晓评信息科技有限公司 技术支持",
                style = MaterialTheme.typography.labelSmall,
                color = ErpColors.TextSecondary,
                textAlign = TextAlign.Center,
            )
        }
    }
}
