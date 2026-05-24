package com.acepanel.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.acepanel.app.data.DatabaseServerApiItem
import com.acepanel.app.data.DatabaseUserApiItem
import com.acepanel.app.ui.components.AnimatedAppDialog
import com.acepanel.app.ui.components.StatusBarSpacer
import com.acepanel.app.viewmodel.DatabaseUserViewModel

@Composable
fun DatabaseUserScreen(
    panelId: String = "",
    onBack: () -> Unit = {},
    showStatusBarSpacer: Boolean = true,
    showBackButton: Boolean = true
) {
    val vm: DatabaseUserViewModel = viewModel()
    val users by vm.users.collectAsStateWithLifecycle()
    val servers by vm.servers.collectAsStateWithLifecycle()
    val isLoading by vm.isLoading.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    val actionId by vm.actionId.collectAsStateWithLifecycle()
    val actionError by vm.actionError.collectAsStateWithLifecycle()
    val showCreateDialog by vm.showCreateDialog.collectAsStateWithLifecycle()
    val editUser by vm.editUser.collectAsStateWithLifecycle()

    LaunchedEffect(panelId) {
        if (panelId.isNotEmpty()) vm.init(panelId)
    }

    editUser?.let { user ->
        EditUserDialog(
            user = user,
            actionError = actionError,
            onDismiss = { vm.hideEdit() },
            onUpdate = { password, privileges -> vm.updateUser(user.id, password, privileges) }
        )
    }

    if (showCreateDialog) {
        CreateUserDialog(
            servers = servers,
            onDismiss = { vm.hideCreate() },
            onCreate = { serverId, username, password, host ->
                vm.createUser(serverId, username, password, host, emptyList())
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
        if (showStatusBarSpacer) StatusBarSpacer()
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showBackButton) {
                Box(
                    modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
                        .border(1.5.dp, Color(0xFFE4E4E7), RoundedCornerShape(10.dp))
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) { Text("‹", fontSize = 20.sp, color = Color(0xFF18181B)) }
            } else {
                Spacer(modifier = Modifier.size(36.dp))
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("数据库用户", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF18181B))
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

        if (error != null) Text("加载失败: $error", fontSize = 12.sp, color = Color(0xFFEF4444),
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp))
        if (actionError != null) Text(actionError!!, fontSize = 12.sp, color = Color(0xFFEF4444),
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp))

        if (users.isEmpty() && !isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(if (panelId.isEmpty()) "请先选择面板" else "暂无数据库用户", fontSize = 14.sp, color = Color(0xFFA1A1AA))
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                items(users) { user ->
                    DatabaseUserRow(
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
private fun DatabaseUserRow(user: DatabaseUserApiItem, isActing: Boolean, onEdit: () -> Unit, onDelete: () -> Unit) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AnimatedAppDialog(onDismissRequest = { showDeleteConfirm = false }) {
            Column(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                    .background(Color.White).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("确认删除", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF18181B))
                Text("删除用户 \"${user.username}\"？", fontSize = 14.sp, color = Color(0xFF71717A))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { showDeleteConfirm = false }) { Text("取消", color = Color(0xFF71717A)) }
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Color(0xFFEF4444))
                            .clickable { showDeleteConfirm = false; onDelete() }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) { Text("删除", fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Medium) }
                }
            }
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(user.username, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF18181B))
            val hostInfo = if (user.host.isNotEmpty()) "@${user.host}" else ""
            Text("服务器ID: ${user.server_id}$hostInfo", fontSize = 12.sp, color = Color(0xFF71717A))
            if (user.privileges.isNotEmpty()) {
                Text("权限: ${user.privileges.take(3).joinToString(", ")}${if (user.privileges.size > 3) "..." else ""}", fontSize = 11.sp, color = Color(0xFFA1A1AA))
            }
        }

        if (isActing) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color(0xFF2563EB))
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    modifier = Modifier.height(28.dp).clip(RoundedCornerShape(6.dp))
                        .border(1.dp, Color(0xFF2563EB), RoundedCornerShape(6.dp))
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
private fun CreateUserDialog(
    servers: List<DatabaseServerApiItem>,
    onDismiss: () -> Unit,
    onCreate: (Long, String, String, String) -> Unit
) {
    var selectedServerId by remember { mutableStateOf(servers.firstOrNull()?.id ?: 0L) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var host by remember { mutableStateOf("localhost") }

    AnimatedAppDialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                .background(Color.White).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("新建数据库用户", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF18181B))

            if (servers.isNotEmpty()) {
                Text("选择服务器", fontSize = 13.sp, color = Color(0xFF71717A))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    servers.take(4).forEach { s ->
                        Box(
                            modifier = Modifier.height(28.dp)
                                .background(if (selectedServerId == s.id) Color(0xFF2563EB) else Color(0xFFF3F4F6), RoundedCornerShape(6.dp))
                                .clickable { selectedServerId = s.id }
                                .padding(horizontal = 10.dp),
                            contentAlignment = Alignment.Center
                        ) { Text(s.name, fontSize = 12.sp, color = if (selectedServerId == s.id) Color.White else Color(0xFF71717A)) }
                    }
                }
            } else {
                Text("请先添加数据库服务器", fontSize = 13.sp, color = Color(0xFFEF4444))
            }

            OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("用户名") },
                modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("密码") },
                modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = host, onValueChange = { host = it }, label = { Text("来源主机") },
                placeholder = { Text("localhost 或 %") },
                modifier = Modifier.fillMaxWidth(), singleLine = true)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("取消", color = Color(0xFF71717A)) }
                Spacer(Modifier.width(8.dp))
                val canSubmit = username.isNotBlank() && password.isNotBlank() && selectedServerId > 0L
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(8.dp))
                        .background(if (canSubmit) Color(0xFF2563EB) else Color(0xFF71717A))
                        .clickable(enabled = canSubmit) { onCreate(selectedServerId, username.trim(), password, host.trim()) }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) { Text("创建", fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Medium) }
            }
        }
    }
}

@Composable
private fun EditUserDialog(
    user: DatabaseUserApiItem,
    actionError: String?,
    onDismiss: () -> Unit,
    onUpdate: (String, List<String>) -> Unit
) {
    var password by remember { mutableStateOf("") }

    AnimatedAppDialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                .background(Color.White).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("修改用户 ${user.username}", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF18181B))

            Text("服务器ID: ${user.server_id}", fontSize = 13.sp, color = Color(0xFF71717A))

            OutlinedTextField(value = password, onValueChange = { password = it },
                label = { Text("新密码（留空不修改）") }, modifier = Modifier.fillMaxWidth(), singleLine = true)

            if (actionError != null) {
                Text(actionError, fontSize = 12.sp, color = Color(0xFFEF4444))
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("取消", color = Color(0xFF71717A)) }
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF2563EB))
                        .clickable { onUpdate(password, user.privileges) }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) { Text("保存", fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Medium) }
            }
        }
    }
}
