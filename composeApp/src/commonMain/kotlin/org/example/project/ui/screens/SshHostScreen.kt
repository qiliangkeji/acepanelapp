package org.example.project.ui.screens

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
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.example.project.data.SshHostItem
import org.example.project.ui.components.StatusBarSpacer
import org.example.project.viewmodel.SshHostViewModel

@Composable
fun SshHostScreen(
    panelId: String = "",
    onBack: () -> Unit = {},
    onConnect: (Long) -> Unit = {}
) {
    val vm: SshHostViewModel = viewModel()
    val hosts by vm.hosts.collectAsStateWithLifecycle()
    val isLoading by vm.isLoading.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    val actionId by vm.actionId.collectAsStateWithLifecycle()
    val actionError by vm.actionError.collectAsStateWithLifecycle()
    val showCreateDialog by vm.showCreateDialog.collectAsStateWithLifecycle()
    val editHost by vm.editHost.collectAsStateWithLifecycle()

    LaunchedEffect(panelId) {
        if (panelId.isNotEmpty()) vm.init(panelId)
    }

    if (showCreateDialog) {
        CreateSshDialog(
            onDismiss = { vm.hideCreate() },
            onCreate = { name, host, port, authMethod, user, password, key ->
                vm.createHost(name, host, port, authMethod, user, password, key)
            }
        )
    }

    editHost?.let { host ->
        EditSshDialog(
            host = host,
            actionError = actionError,
            onDismiss = { vm.hideEdit() },
            onUpdate = { name, h, port, authMethod, user, password, key ->
                vm.updateHost(host.id, name, h, port, authMethod, user, password, key)
            }
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
                Text("SSH 主机", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF18181B))
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

        if (hosts.isEmpty() && !isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(if (panelId.isEmpty()) "请先选择面板" else "暂无 SSH 主机", fontSize = 14.sp, color = Color(0xFFA1A1AA))
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                items(hosts) { host ->
                    SshHostRow(
                        host = host,
                        isActing = actionId == host.id,
                        onConnect = { onConnect(host.id) },
                        onEdit = { vm.showEdit(host) },
                        onDelete = { vm.deleteHost(host.id) }
                    )
                    HorizontalDivider(thickness = 0.5.dp, color = Color(0xFFF0F0F0))
                }
            }
        }
    }
}

@Composable
private fun SshHostRow(host: SshHostItem, isActing: Boolean, onConnect: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        Dialog(onDismissRequest = { showDeleteConfirm = false }) {
            Column(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                    .background(Color.White).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("确认删除", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF18181B))
                Text("删除主机 \"${host.name}\"？", fontSize = 14.sp, color = Color(0xFF71717A))
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
            Text(host.name.ifEmpty { host.host }, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF18181B))
            Text("${host.config.user.ifEmpty { "?" }}@${host.host}:${host.port}", fontSize = 12.sp, color = Color(0xFF71717A))
            val authLabel = when (host.config.auth_method) {
                "publickey" -> "密钥认证"
                else -> "密码认证"
            }
            Text(authLabel, fontSize = 11.sp, color = Color(0xFFA1A1AA))
        }

        if (isActing) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color(0xFF2563EB))
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    modifier = Modifier.height(28.dp).clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF2563EB))
                        .clickable { onConnect() }
                        .padding(horizontal = 10.dp),
                    contentAlignment = Alignment.Center
                ) { Text("终端", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Medium) }
                Box(
                    modifier = Modifier.height(28.dp).clip(RoundedCornerShape(6.dp))
                        .border(1.dp, Color(0xFF2563EB), RoundedCornerShape(6.dp))
                        .clickable { onEdit() }
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
private fun CreateSshDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String, Int, String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var host by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("22") }
    var authMethod by remember { mutableStateOf("password") }
    var user by remember { mutableStateOf("root") }
    var password by remember { mutableStateOf("") }
    var key by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                .background(Color.White).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("新建 SSH 主机", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF18181B))

            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("名称") },
                modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = host, onValueChange = { host = it }, label = { Text("主机地址") },
                modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = port, onValueChange = { port = it }, label = { Text("端口") },
                modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = user, onValueChange = { user = it }, label = { Text("用户名") },
                modifier = Modifier.fillMaxWidth(), singleLine = true)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("password" to "密码", "publickey" to "密钥").forEach { (method, label) ->
                    Box(
                        modifier = Modifier.height(32.dp)
                            .background(if (authMethod == method) Color(0xFF2563EB) else Color(0xFFF3F4F6), RoundedCornerShape(8.dp))
                            .clickable { authMethod = method }
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center
                    ) { Text(label, fontSize = 13.sp, color = if (authMethod == method) Color.White else Color(0xFF71717A)) }
                }
            }

            if (authMethod == "password") {
                OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("密码") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true)
            } else {
                OutlinedTextField(value = key, onValueChange = { key = it }, label = { Text("私钥内容") },
                    modifier = Modifier.fillMaxWidth().height(100.dp), maxLines = 5)
            }

            Text("注意：保存时会做真实 SSH 连通性检查", fontSize = 12.sp, color = Color(0xFF71717A))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("取消", color = Color(0xFF71717A)) }
                Spacer(Modifier.width(8.dp))
                val portInt = port.trim().toIntOrNull() ?: 0
                val canSubmit = name.isNotBlank() && host.isNotBlank() && portInt > 0 && user.isNotBlank()
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(8.dp))
                        .background(if (canSubmit) Color(0xFF2563EB) else Color(0xFF71717A))
                        .clickable(enabled = canSubmit) {
                            onCreate(name.trim(), host.trim(), portInt, authMethod, user.trim(), password, key)
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) { Text("创建", fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Medium) }
            }
        }
    }
}

@Composable
private fun EditSshDialog(
    host: SshHostItem,
    actionError: String?,
    onDismiss: () -> Unit,
    onUpdate: (String, String, Int, String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf(host.name) }
    var hostAddr by remember { mutableStateOf(host.host) }
    var port by remember { mutableStateOf(host.port.toString()) }
    var authMethod by remember { mutableStateOf(host.config.auth_method.ifEmpty { "password" }) }
    var user by remember { mutableStateOf(host.config.user) }
    var password by remember { mutableStateOf("") }
    var key by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                .background(Color.White).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("编辑 SSH 主机", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF18181B))

            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("名称") },
                modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = hostAddr, onValueChange = { hostAddr = it }, label = { Text("主机地址") },
                modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = port, onValueChange = { port = it }, label = { Text("端口") },
                modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = user, onValueChange = { user = it }, label = { Text("用户名") },
                modifier = Modifier.fillMaxWidth(), singleLine = true)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("password" to "密码", "publickey" to "密钥").forEach { (method, label) ->
                    Box(
                        modifier = Modifier.height(32.dp)
                            .background(if (authMethod == method) Color(0xFF2563EB) else Color(0xFFF3F4F6), RoundedCornerShape(8.dp))
                            .clickable { authMethod = method }
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center
                    ) { Text(label, fontSize = 13.sp, color = if (authMethod == method) Color.White else Color(0xFF71717A)) }
                }
            }

            if (authMethod == "password") {
                OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("新密码（留空不修改）") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true)
            } else {
                OutlinedTextField(value = key, onValueChange = { key = it }, label = { Text("私钥内容（留空不修改）") },
                    modifier = Modifier.fillMaxWidth().height(100.dp), maxLines = 5)
            }

            if (actionError != null) {
                Text(actionError, fontSize = 12.sp, color = Color(0xFFEF4444))
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("取消", color = Color(0xFF71717A)) }
                Spacer(Modifier.width(8.dp))
                val portInt = port.trim().toIntOrNull() ?: 0
                val canSubmit = name.isNotBlank() && hostAddr.isNotBlank() && portInt > 0 && user.isNotBlank()
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(8.dp))
                        .background(if (canSubmit) Color(0xFF2563EB) else Color(0xFF71717A))
                        .clickable(enabled = canSubmit) {
                            onUpdate(name.trim(), hostAddr.trim(), portInt, authMethod, user.trim(), password, key)
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) { Text("保存", fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Medium) }
            }
        }
    }
}
