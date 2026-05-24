package com.acepanel.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.acepanel.app.ui.components.StatusBarSpacer
import com.acepanel.app.viewmodel.AddPanelViewModel

@Composable
fun AddPanelScreen(
    panelId: String? = null,
    onBackClick: () -> Unit = {},
    onSaveClick: () -> Unit = {},
    onSessionSaved: (panelId: String, username: String, password: String, autoLogin: Boolean) -> Unit = { _, _, _, _ -> }
) {
    val vm: AddPanelViewModel = viewModel()
    val name by vm.name.collectAsStateWithLifecycle()
    val scheme by vm.scheme.collectAsStateWithLifecycle()
    val host by vm.host.collectAsStateWithLifecycle()
    val port by vm.port.collectAsStateWithLifecycle()
    val entrance by vm.entrance.collectAsStateWithLifecycle()
    val authMode by vm.authMode.collectAsStateWithLifecycle()
    val quickPaste by vm.quickPaste.collectAsStateWithLifecycle()
    val userAgent by vm.userAgent.collectAsStateWithLifecycle()
    val tokenId by vm.tokenId.collectAsStateWithLifecycle()
    val tokenSecret by vm.tokenSecret.collectAsStateWithLifecycle()
    val remark by vm.remark.collectAsStateWithLifecycle()
    val testStatus by vm.testStatus.collectAsStateWithLifecycle()
    val isEditMode = !panelId.isNullOrBlank()

    LaunchedEffect(panelId) {
        vm.init(panelId)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        StatusBarSpacer()
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.5.dp, Color(0xFFE4E4E7), RoundedCornerShape(10.dp))
                    .clickable(onClick = onBackClick),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "‹", fontSize = 20.sp, color = Color(0xFF18181B))
            }
            Text(
                text = if (isEditMode) "编辑面板" else "添加面板",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF18181B)
            )
            Box(modifier = Modifier.size(36.dp))
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                InputField(
                    label = "粘贴面板信息自动登录",
                    value = quickPaste,
                    onValueChange = { text ->
                        vm.onQuickPasteChanged(text) { panelId, username, password ->
                            onSessionSaved(panelId, username, password, true)
                        }
                    },
                    placeholder = if (isEditMode) "可粘贴新的面板信息覆盖当前配置" else "粘贴包含用户名、密码、端口、入口、本地 IPv4/公网 IPv4 的文本",
                    minLines = 5
                )
            }

            item {
                InputField(
                    label = "面板名称",
                    value = name,
                    onValueChange = { vm.name.value = it },
                    placeholder = "例：生产服务器"
                )
            }

            // 协议 + 地址 + 端口
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LabelText("连接地址")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 协议选择
                        Box(
                            modifier = Modifier
                                .height(48.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .border(1.5.dp, Color(0xFFE4E4E7), RoundedCornerShape(10.dp))
                                .clickable {
                                    vm.scheme.value = if (scheme == "http") "https" else "http"
                                }
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = scheme,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF2563EB)
                            )
                        }
                        // 主机
                        OutlinedTextField(
                            value = host,
                            onValueChange = { vm.host.value = it },
                            placeholder = { Text("192.168.1.100", color = Color(0xFFA1A1AA)) },
                            modifier = Modifier.weight(1f).height(48.dp),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            colors = outlinedTextFieldColors()
                        )
                        // 端口
                        OutlinedTextField(
                            value = port,
                            onValueChange = { vm.port.value = it },
                            placeholder = { Text("8888", color = Color(0xFFA1A1AA)) },
                            modifier = Modifier.width(80.dp).height(48.dp),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = outlinedTextFieldColors()
                        )
                    }
                }
            }

            item {
                InputField(
                    label = "安全入口（面板开启时必填）",
                    value = entrance,
                    onValueChange = { vm.entrance.value = it },
                    placeholder = "例：my-panel 或 /my-panel；入口为 / 时可留空"
                )
            }

            item {
                InputField(
                    label = "User-Agent（UA 绑定时填写）",
                    value = userAgent,
                    onValueChange = { vm.userAgent.value = it },
                    placeholder = "留空使用 Chrome UA；浏览器能打开但客户端 418 时填浏览器 UA",
                    minLines = 2
                )
            }

            // 鉴权模式切换
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LabelText("登录方式")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.5.dp, Color(0xFFE4E4E7), RoundedCornerShape(10.dp)),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        AuthModeTab(
                            label = "账号密码",
                            selected = authMode == "session",
                            onClick = { vm.authMode.value = "session" },
                            modifier = Modifier.weight(1f)
                        )
                        AuthModeTab(
                            label = "Token",
                            selected = authMode == "token",
                            onClick = { vm.authMode.value = "token" },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Token 模式字段
            if (authMode == "token") {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        InputField(
                            label = "Token ID",
                            value = tokenId,
                            onValueChange = { vm.tokenId.value = it },
                            placeholder = "在面板「设置→API密钥」中查看，例如：1",
                            keyboardType = KeyboardType.Number
                        )
                        Text(
                            text = "⚠ Token ID 是数字编号，不是用户名",
                            fontSize = 11.sp,
                            color = Color(0xFF71717A)
                        )
                    }
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        InputField(
                            label = "Token Secret",
                            value = tokenSecret,
                            onValueChange = { vm.tokenSecret.value = it },
                            placeholder = "创建 API密钥时面板显示的密钥字符串",
                            isPassword = true
                        )
                        Text(
                            text = "⚠ Token Secret 是 API 密钥，不是登录密码",
                            fontSize = 11.sp,
                            color = Color(0xFF71717A)
                        )
                    }
                }
            } else {
                item {
                    Text(
                        text = "账号密码登录：保存后会跳转到登录页，使用面板用户名和密码登录。终端功能必须使用此模式。",
                        fontSize = 13.sp,
                        color = Color(0xFF71717A),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFF9FAFB))
                            .padding(12.dp)
                    )
                }
            }

            item {
                InputField(
                    label = "备注（可选）",
                    value = remark,
                    onValueChange = { vm.remark.value = it },
                    placeholder = "添加备注信息",
                    minLines = 2
                )
            }
        }

        // 测试连接结果 — 在按钮上方，始终可见
        if (testStatus !is AddPanelViewModel.TestStatus.Idle) {
            TestStatusCard(
                testStatus = testStatus,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
            )
        }

        // 操作按钮
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 测试连接
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.5.dp, Color(0xFF2563EB), RoundedCornerShape(12.dp))
                    .clickable(
                        enabled = testStatus !is AddPanelViewModel.TestStatus.Testing
                    ) { vm.testConnection() },
                contentAlignment = Alignment.Center
            ) {
                if (testStatus is AddPanelViewModel.TestStatus.Testing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = Color(0xFF2563EB)
                    )
                } else {
                    Text(
                        text = "测试连接",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF2563EB)
                    )
                }
            }

            // 保存
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF2563EB))
                    .clickable {
                    vm.savePanel(
                        onTokenSaved = onSaveClick,
                        onSessionSaved = { panelId, username ->
                            if (isEditMode) {
                                onSaveClick()
                            } else {
                                onSessionSaved(panelId, username, "", false)
                            }
                        }
                    )
                },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isEditMode) "保存修改" else "保存",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun TestStatusCard(testStatus: AddPanelViewModel.TestStatus, modifier: Modifier = Modifier) {
    val (bgColor, textColor, message) = when (testStatus) {
        is AddPanelViewModel.TestStatus.Success ->
            Triple(Color(0xFFECFDF5), Color(0xFF10B981), testStatus.message)
        is AddPanelViewModel.TestStatus.Error ->
            Triple(Color(0xFFFEF2F2), Color(0xFFEF4444), testStatus.message)
        else -> Triple(Color(0xFFF9FAFB), Color(0xFF71717A), "")
    }
    if (message.isNotEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(bgColor)
                .padding(12.dp)
        ) {
            Text(text = message, fontSize = 13.sp, color = textColor)
        }
    }
}

@Composable
private fun AuthModeTab(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) Color(0xFF2563EB) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) Color.White else Color(0xFF71717A)
        )
    }
}

@Composable
private fun LabelText(text: String) {
    Text(
        text = text,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color(0xFF71717A),
        letterSpacing = 0.5.sp
    )
}

@Composable
fun InputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    minLines: Int = 1
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        LabelText(label)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = Color(0xFFA1A1AA), fontSize = 14.sp) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = minLines == 1,
            minLines = minLines,
            visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            shape = RoundedCornerShape(10.dp),
            colors = outlinedTextFieldColors()
        )
    }
}

@Composable
private fun outlinedTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Color(0xFF2563EB),
    unfocusedBorderColor = Color(0xFFE4E4E7),
    focusedTextColor = Color(0xFF18181B),
    unfocusedTextColor = Color(0xFF18181B),
    cursorColor = Color(0xFF2563EB)
)

// 保持向后兼容的旧 FormItem（其他页面仍使用）
@Composable
fun FormItem(
    label: String,
    placeholder: String,
    height: androidx.compose.ui.unit.Dp = 48.dp
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF71717A),
            letterSpacing = 1.sp
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .clip(RoundedCornerShape(12.dp))
                .border(1.5.dp, Color(0xFFE4E4E7), RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            contentAlignment = if (height > 48.dp) Alignment.TopStart else Alignment.CenterStart
        ) {
            Text(
                text = placeholder,
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFFA1A1AA)
            )
        }
    }
}
