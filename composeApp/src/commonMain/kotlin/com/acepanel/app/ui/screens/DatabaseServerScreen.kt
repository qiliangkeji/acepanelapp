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
import com.acepanel.app.ui.components.AnimatedAppDialog
import com.acepanel.app.ui.components.StatusBarSpacer
import com.acepanel.app.viewmodel.DatabaseServerViewModel

@Composable
fun DatabaseServerScreen(
    panelId: String = "",
    onBack: () -> Unit = {},
    showStatusBarSpacer: Boolean = true,
    showBackButton: Boolean = true
) {
    val vm: DatabaseServerViewModel = viewModel()
    val servers by vm.servers.collectAsStateWithLifecycle()
    val isLoading by vm.isLoading.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    val actionId by vm.actionId.collectAsStateWithLifecycle()
    val actionError by vm.actionError.collectAsStateWithLifecycle()
    val showCreateDialog by vm.showCreateDialog.collectAsStateWithLifecycle()
    val editServer by vm.showEditDialog.collectAsStateWithLifecycle()

    LaunchedEffect(panelId) {
        if (panelId.isNotEmpty()) vm.init(panelId)
    }

    if (showCreateDialog) {
        CreateServerDialog(
            onDismiss = { vm.hideCreate() },
            onCreate = { name, type, host, port, username, password, remark ->
                vm.createServer(name, type, host, port, username, password, remark)
            }
        )
    }

    if (editServer != null) {
        EditServerDialog(
            server = editServer!!,
            onDismiss = { vm.hideEdit() },
            onSave = { name, host, port, username, password, remark ->
                vm.updateServer(editServer!!.id, name, host, port, username, password.ifBlank { editServer!!.password }, remark)
            }
        )
    }

    Column(
        modifier = Modifier.fillMaxSize().background(Color.White)
    ) {
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
                Text("数据库服务器", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF18181B))
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

        if (servers.isEmpty() && !isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(if (panelId.isEmpty()) "请先选择面板" else "暂无数据库服务器", fontSize = 14.sp, color = Color(0xFFA1A1AA))
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(servers) { server ->
                    ServerCard(
                        server = server,
                        isActing = actionId == server.id,
                        onEdit = { vm.showEdit(server) },
                        onDelete = { vm.deleteServer(server.id) },
                        onSync = { vm.syncServer(server.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ServerCard(
    server: DatabaseServerApiItem,
    isActing: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSync: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AnimatedAppDialog(onDismissRequest = { showDeleteConfirm = false }) {
            Column(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                    .background(Color.White).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("确认删除", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF18181B))
                Text("删除 \"${server.name}\"？仅删除面板记录，不影响实际数据库。", fontSize = 14.sp, color = Color(0xFF71717A))
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

    Column(
        modifier = Modifier.fillMaxWidth()
            .border(1.5.dp, Color(0xFFE4E4E7), RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(server.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF18181B))
                Text("${server.host}:${server.port}", fontSize = 12.sp, color = Color(0xFF71717A))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                val (bg, tc, label) = when (server.type.lowercase()) {
                    "mysql" -> Triple(Color(0xFFEFF6FF), Color(0xFF2563EB), "MySQL")
                    "postgresql" -> Triple(Color(0xFFF0FDF4), Color(0xFF059669), "PostgreSQL")
                    "redis" -> Triple(Color(0xFFFFF7ED), Color(0xFFEA580C), "Redis")
                    else -> Triple(Color(0xFFF3F4F6), Color(0xFF71717A), server.type)
                }
                Box(
                    modifier = Modifier.background(bg, RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 3.dp)
                ) { Text(label, fontSize = 11.sp, color = tc, fontWeight = FontWeight.SemiBold) }
            }
        }

        if (server.remark.isNotEmpty()) {
            Text(server.remark, fontSize = 12.sp, color = Color(0xFFA1A1AA))
        }

        HorizontalDivider(thickness = 1.dp, color = Color(0xFFE4E4E7))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (isActing) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color(0xFF2563EB))
            } else {
                Box(
                    modifier = Modifier.weight(1f).height(32.dp).clip(RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFF2563EB), RoundedCornerShape(8.dp))
                        .clickable(onClick = onEdit),
                    contentAlignment = Alignment.Center
                ) { Text("编辑", fontSize = 12.sp, color = Color(0xFF2563EB), fontWeight = FontWeight.Medium) }
                Box(
                    modifier = Modifier.weight(1f).height(32.dp).clip(RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFF2563EB), RoundedCornerShape(8.dp))
                        .clickable(onClick = onSync),
                    contentAlignment = Alignment.Center
                ) { Text("同步用户", fontSize = 12.sp, color = Color(0xFF2563EB), fontWeight = FontWeight.Medium) }
                Box(
                    modifier = Modifier.weight(1f).height(32.dp).clip(RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFFEF4444), RoundedCornerShape(8.dp))
                        .clickable { showDeleteConfirm = true },
                    contentAlignment = Alignment.Center
                ) { Text("删除", fontSize = 12.sp, color = Color(0xFFEF4444), fontWeight = FontWeight.Medium) }
            }
        }
    }
}

@Composable
private fun CreateServerDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String, String, Int, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("mysql") }
    var host by remember { mutableStateOf("127.0.0.1") }
    var port by remember { mutableStateOf("3306") }
    var username by remember { mutableStateOf("root") }
    var password by remember { mutableStateOf("") }
    var remark by remember { mutableStateOf("") }

    val typeItems = listOf("mysql" to "MySQL", "postgresql" to "PostgreSQL", "redis" to "Redis")

    AnimatedAppDialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                .background(Color.White).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("新建数据库服务器", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF18181B))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                typeItems.forEach { (key, label) ->
                    Box(
                        modifier = Modifier.height(32.dp)
                            .background(if (type == key) Color(0xFF2563EB) else Color(0xFFF3F4F6), RoundedCornerShape(8.dp))
                            .clickable {
                                type = key
                                port = when (key) { "postgresql" -> "5432"; "redis" -> "6379"; else -> "3306" }
                            }
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center
                    ) { Text(label, fontSize = 13.sp, color = if (type == key) Color.White else Color(0xFF71717A)) }
                }
            }

            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("名称") },
                modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = host, onValueChange = { host = it }, label = { Text("主机") },
                modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = port, onValueChange = { port = it }, label = { Text("端口") },
                modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("用户名") },
                modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("密码") },
                modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = remark, onValueChange = { remark = it }, label = { Text("备注（可选）") },
                modifier = Modifier.fillMaxWidth(), singleLine = true)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("取消", color = Color(0xFF71717A)) }
                Spacer(Modifier.width(8.dp))
                val portInt = port.trim().toIntOrNull() ?: 0
                val canSubmit = name.isNotBlank() && host.isNotBlank() && portInt > 0 && username.isNotBlank()
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(8.dp))
                        .background(if (canSubmit) Color(0xFF2563EB) else Color(0xFF71717A))
                        .clickable(enabled = canSubmit) { onCreate(name.trim(), type, host.trim(), portInt, username.trim(), password, remark.trim()) }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) { Text("创建", fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Medium) }
            }
        }
    }
}

@Composable
private fun EditServerDialog(
    server: DatabaseServerApiItem,
    onDismiss: () -> Unit,
    onSave: (String, String, Int, String, String, String) -> Unit
) {
    var name by remember(server.id) { mutableStateOf(server.name) }
    var host by remember(server.id) { mutableStateOf(server.host) }
    var port by remember(server.id) { mutableStateOf(server.port.toString()) }
    var username by remember(server.id) { mutableStateOf(server.username) }
    var password by remember(server.id) { mutableStateOf("") }
    var remark by remember(server.id) { mutableStateOf(server.remark) }

    AnimatedAppDialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                .background(Color.White).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("编辑数据库服务器", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF18181B))
            Text("类型：${server.type}", fontSize = 12.sp, color = Color(0xFF71717A))
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("名称") },
                modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = host, onValueChange = { host = it }, label = { Text("主机") },
                modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = port, onValueChange = { port = it }, label = { Text("端口") },
                modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("用户名") },
                modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("密码（留空保持不变）") },
                modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = remark, onValueChange = { remark = it }, label = { Text("备注") },
                modifier = Modifier.fillMaxWidth(), singleLine = true)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("取消", color = Color(0xFF71717A)) }
                Spacer(Modifier.width(8.dp))
                val portInt = port.trim().toIntOrNull() ?: 0
                val canSubmit = name.isNotBlank() && host.isNotBlank() && portInt in 1..65535 && username.isNotBlank()
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(8.dp))
                        .background(if (canSubmit) Color(0xFF2563EB) else Color(0xFF71717A))
                        .clickable(enabled = canSubmit) { onSave(name.trim(), host.trim(), portInt, username.trim(), password, remark.trim()) }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) { Text("保存", fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Medium) }
            }
        }
    }
}
