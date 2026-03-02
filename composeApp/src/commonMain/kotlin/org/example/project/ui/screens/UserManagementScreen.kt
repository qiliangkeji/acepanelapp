package org.example.project.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.example.project.data.UserItem
import org.example.project.ui.components.StatusBarSpacer
import org.example.project.viewmodel.UserManagementViewModel

@Composable
fun UserManagementScreen(
    panelId: String = "",
    onBack: () -> Unit = {}
) {
    val vm: UserManagementViewModel = viewModel()
    val users by vm.users.collectAsStateWithLifecycle()
    val isLoading by vm.isLoading.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    val actionId by vm.actionId.collectAsStateWithLifecycle()
    val actionError by vm.actionError.collectAsStateWithLifecycle()
    val showCreateDialog by vm.showCreateDialog.collectAsStateWithLifecycle()
    val editUser by vm.editUser.collectAsStateWithLifecycle()
    val twoFaInfo by vm.twoFaInfo.collectAsStateWithLifecycle()
    val isTwoFaLoading by vm.isTwoFaLoading.collectAsStateWithLifecycle()

    LaunchedEffect(panelId) {
        if (panelId.isNotEmpty()) vm.init(panelId)
    }

    // 新建用户弹窗
    if (showCreateDialog) {
        CreateSystemUserDialog(
            onDismiss = { vm.hideCreate() },
            onCreate = { username, password, email -> vm.createUser(username, password, email) }
        )
    }

    // 编辑用户弹窗
    editUser?.let { user ->
        EditUserDialog(
            user = user,
            actionError = actionError,
            onDismiss = { vm.hideEdit() },
            onUpdateUsername = { vm.updateUsername(user.id, it) },
            onUpdatePassword = { vm.updatePassword(user.id, it) },
            onUpdateEmail = { vm.updateEmail(user.id, it) },
            onOpenTwoFa = { vm.loadTwoFa(user.id) }
        )
    }

    // 2FA 弹窗
    twoFaInfo?.let { info ->
        val userId = editUser?.id ?: 0L
        TwoFaDialog(
            info = info,
            isLoading = isTwoFaLoading,
            actionError = actionError,
            onDismiss = { vm.hideTwoFa() },
            onEnable = { secret, code -> vm.enableTwoFa(userId, secret, code) },
            onDisable = { vm.disableTwoFa(userId) }
        )
    }

    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
        StatusBarSpacer()
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
                    .border(1.5.dp, Color(0xFFE4E4E7), RoundedCornerShape(10.dp))
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) { Text("‹", fontSize = 20.sp, color = Color(0xFF18181B)) }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("用户管理", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF18181B))
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color(0xFF2563EB))
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
                        .border(1.5.dp, Color(0xFFE4E4E7), RoundedCornerShape(10.dp))
                        .clickable { vm.load() },
                    contentAlignment = Alignment.Center
                ) { Text("↻", fontSize = 16.sp, color = Color(0xFF18181B)) }
                Box(
                    modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF2563EB))
                        .clickable { vm.showCreate() },
                    contentAlignment = Alignment.Center
                ) { Text("+", fontSize = 20.sp, color = Color.White, fontWeight = FontWeight.Bold) }
            }
        }

        if (error != null) Text(
            "加载失败: $error", fontSize = 12.sp, color = Color(0xFFEF4444),
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
        )

        if (users.isEmpty() && !isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(if (panelId.isEmpty()) "请先选择面板" else "暂无用户", fontSize = 14.sp, color = Color(0xFFA1A1AA))
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                items(users) { user ->
                    UserRow(
                        user = user,
                        isActing = actionId == user.id,
                        onEdit = { vm.showEdit(user) },
                        onDelete = { vm.deleteUser(user.id) }
                    )
                    HorizontalDivider(thickness = 0.5.dp, color = Color(0xFFF0F0F0))
                }
            }
        }
    }
}

@Composable
private fun UserRow(
    user: UserItem,
    isActing: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("确认删除", fontWeight = FontWeight.SemiBold) },
            text = { Text("删除用户 \"${user.username}\"？最后一个用户无法删除。") },
            confirmButton = {
                TextButton(onClick = { showDeleteConfirm = false; onDelete() }) {
                    Text("删除", color = Color(0xFFEF4444))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("取消", color = Color(0xFF2563EB))
                }
            }
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(user.username, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF18181B))
            if (user.email.isNotEmpty()) Text(user.email, fontSize = 12.sp, color = Color(0xFF71717A))
            if (user.created_at.isNotEmpty()) Text("创建于: ${user.created_at.take(10)}", fontSize = 11.sp, color = Color(0xFFA1A1AA))
        }

        if (isActing) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color(0xFF2563EB))
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    modifier = Modifier.height(28.dp).clip(RoundedCornerShape(6.dp))
                        .border(1.dp, Color(0xFFE4E4E7), RoundedCornerShape(6.dp))
                        .clickable(onClick = onEdit)
                        .padding(horizontal = 10.dp),
                    contentAlignment = Alignment.Center
                ) { Text("编辑", fontSize = 11.sp, color = Color(0xFF2563EB), fontWeight = FontWeight.Medium) }
                Box(
                    modifier = Modifier.height(28.dp).clip(RoundedCornerShape(6.dp))
                        .border(1.dp, Color(0xFFEF4444), RoundedCornerShape(6.dp))
                        .clickable { showDeleteConfirm = true }
                        .padding(horizontal = 10.dp),
                    contentAlignment = Alignment.Center
                ) { Text("删除", fontSize = 11.sp, color = Color(0xFFEF4444), fontWeight = FontWeight.Medium) }
            }
        }
    }
}

@Composable
private fun EditUserDialog(
    user: UserItem,
    actionError: String?,
    onDismiss: () -> Unit,
    onUpdateUsername: (String) -> Unit,
    onUpdatePassword: (String) -> Unit,
    onUpdateEmail: (String) -> Unit,
    onOpenTwoFa: () -> Unit
) {
    var tab by remember { mutableStateOf(0) }  // 0=用户名, 1=密码, 2=邮箱, 3=2FA
    var inputValue by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                .background(Color.White).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("编辑用户: ${user.username}", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF18181B))

            // Tab 选择
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFF4F4F5))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("用户名", "密码", "邮箱", "2FA").forEachIndexed { index, label ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (tab == index) Color.White else Color.Transparent)
                            .clickable { tab = index; inputValue = "" }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            label,
                            fontSize = 12.sp,
                            fontWeight = if (tab == index) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (tab == index) Color(0xFF18181B) else Color(0xFF71717A)
                        )
                    }
                }
            }

            if (actionError != null) {
                Text(actionError, fontSize = 12.sp, color = Color(0xFFEF4444))
            }

            when (tab) {
                0, 1, 2 -> {
                    val (label, hint) = when (tab) {
                        0 -> "新用户名" to "字母数字下划线横线"
                        1 -> "新密码" to "8-20位，至少两类字符"
                        else -> "新邮箱" to "合法邮箱地址"
                    }
                    OutlinedTextField(
                        value = inputValue,
                        onValueChange = { inputValue = it },
                        label = { Text(label) },
                        placeholder = { Text(hint, fontSize = 12.sp, color = Color(0xFFA1A1AA)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF2563EB),
                            unfocusedBorderColor = Color(0xFFE4E4E7)
                        )
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = onDismiss) { Text("取消", color = Color(0xFF71717A)) }
                        val canSubmit = when (tab) {
                            0 -> inputValue.matches(Regex("^[a-zA-Z0-9_-]+$")) && inputValue.length >= 2
                            1 -> inputValue.length >= 8
                            else -> inputValue.contains("@")
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (canSubmit) Color(0xFF2563EB) else Color(0xFFA1A1AA))
                                .clickable(enabled = canSubmit) {
                                    when (tab) {
                                        0 -> onUpdateUsername(inputValue.trim())
                                        1 -> onUpdatePassword(inputValue)
                                        else -> onUpdateEmail(inputValue.trim())
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text("保存", fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Medium)
                        }
                    }
                }
                3 -> {
                    // 2FA 入口
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "TOTP 两步验证（Time-based One-Time Password）",
                            fontSize = 13.sp,
                            color = Color(0xFF71717A)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFEFF6FF))
                                .clickable(onClick = onOpenTwoFa)
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "获取 2FA 二维码",
                                fontSize = 14.sp,
                                color = Color(0xFF2563EB),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = onDismiss) { Text("关闭", color = Color(0xFF71717A)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun TwoFaDialog(
    info: org.example.project.data.TwoFaInfo,
    isLoading: Boolean,
    actionError: String?,
    onDismiss: () -> Unit,
    onEnable: (secret: String, code: String) -> Unit,
    onDisable: () -> Unit
) {
    var code by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                .background(Color.White).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("配置双因素认证", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF18181B))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF2563EB))
                }
            } else {
                if (actionError != null) {
                    Text(actionError, fontSize = 12.sp, color = Color(0xFFEF4444))
                }
                Text("使用 Google Authenticator 等应用扫描以下二维码地址，或手动输入密钥：", fontSize = 13.sp, color = Color(0xFF71717A))
                // TOTP Secret
                Box(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF4F4F5)).padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("TOTP 密钥", fontSize = 11.sp, color = Color(0xFF71717A))
                        Text(info.secret, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF18181B))
                    }
                }
                // otpauth URL (truncated)
                if (info.url.isNotEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFF4F4F5)).padding(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("二维码链接", fontSize = 11.sp, color = Color(0xFF71717A))
                            Text(
                                if (info.url.length > 60) "${info.url.take(60)}..." else info.url,
                                fontSize = 12.sp,
                                color = Color(0xFF2563EB)
                            )
                        }
                    }
                }
                // 验证码输入
                OutlinedTextField(
                    value = code,
                    onValueChange = { if (it.all { c -> c.isDigit() } && it.length <= 6) code = it },
                    label = { Text("6位验证码（启用用）") },
                    placeholder = { Text("000000", color = Color(0xFFA1A1AA)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF2563EB),
                        unfocusedBorderColor = Color(0xFFE4E4E7)
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFFEF4444), RoundedCornerShape(8.dp))
                            .clickable(onClick = onDisable)
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) { Text("关闭 2FA", fontSize = 13.sp, color = Color(0xFFEF4444)) }
                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(8.dp))
                            .background(if (code.length == 6) Color(0xFF2563EB) else Color(0xFFA1A1AA))
                            .clickable(enabled = code.length == 6) { onEnable(info.secret, code) }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) { Text("启用 2FA", fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Medium) }
                    TextButton(onClick = onDismiss) { Text("取消", color = Color(0xFF71717A)) }
                }
            }
        }
    }
}

@Composable
private fun CreateSystemUserDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String, String) -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                .background(Color.White).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("新建用户", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF18181B))
            Text("用户名: 字母数字下划线横线；密码: 8-20位且至少两类字符", fontSize = 12.sp, color = Color(0xFF71717A))
            OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("用户名") },
                modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("密码（8-20位）") },
                modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("邮箱") },
                modifier = Modifier.fillMaxWidth(), singleLine = true)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("取消", color = Color(0xFF71717A)) }
                Spacer(Modifier.width(8.dp))
                val canSubmit = username.isNotBlank() && password.length >= 8 && email.contains("@")
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(8.dp))
                        .background(if (canSubmit) Color(0xFF2563EB) else Color(0xFF71717A))
                        .clickable(enabled = canSubmit) { onCreate(username.trim(), password, email.trim()) }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) { Text("创建", fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Medium) }
            }
        }
    }
}
