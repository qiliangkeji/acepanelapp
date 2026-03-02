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
import org.example.project.data.BackupStorageInfo
import org.example.project.data.BackupStorageItem
import org.example.project.ui.components.StatusBarSpacer
import org.example.project.viewmodel.BackupStorageViewModel

@Composable
fun BackupStorageScreen(
    panelId: String = "",
    onBack: () -> Unit = {}
) {
    val vm: BackupStorageViewModel = viewModel()
    val storages by vm.storages.collectAsStateWithLifecycle()
    val isLoading by vm.isLoading.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    val actionId by vm.actionId.collectAsStateWithLifecycle()
    val actionError by vm.actionError.collectAsStateWithLifecycle()
    val showCreateDialog by vm.showCreateDialog.collectAsStateWithLifecycle()
    val editStorage by vm.editStorage.collectAsStateWithLifecycle()

    LaunchedEffect(panelId) {
        if (panelId.isNotEmpty()) vm.init(panelId)
    }

    if (showCreateDialog) {
        CreateStorageDialog(
            onDismiss = { vm.hideCreate() },
            onCreate = { type, name, info -> vm.createStorage(type, name, info) }
        )
    }

    editStorage?.let { storage ->
        EditStorageDialog(
            storage = storage,
            actionError = actionError,
            onDismiss = { vm.hideEdit() },
            onUpdate = { type, name, info -> vm.updateStorage(storage.id, type, name, info) }
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
                Text("备份存储", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF18181B))
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

        if (storages.isEmpty() && !isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(if (panelId.isEmpty()) "请先选择面板" else "暂无备份存储", fontSize = 14.sp, color = Color(0xFFA1A1AA))
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                items(storages) { storage ->
                    StorageRow(
                        storage = storage,
                        isActing = actionId == storage.id,
                        onEdit = { if (storage.id > 0) vm.showEdit(storage) },
                        onDelete = { if (storage.id > 0) vm.deleteStorage(storage.id) }
                    )
                    HorizontalDivider(thickness = 0.5.dp, color = Color(0xFFF0F0F0))
                }
            }
        }
    }
}

@Composable
private fun StorageRow(storage: BackupStorageItem, isActing: Boolean, onEdit: () -> Unit, onDelete: () -> Unit) {
    val isLocal = storage.id == 0L || storage.type == "local"
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        Dialog(onDismissRequest = { showDeleteConfirm = false }) {
            Column(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                    .background(Color.White).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("确认删除", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF18181B))
                Text("删除存储 \"${storage.name}\"？", fontSize = 14.sp, color = Color(0xFF71717A))
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
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(storage.name, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF18181B))
                val (bg, tc, label) = when (storage.type) {
                    "s3" -> Triple(Color(0xFFFFF7ED), Color(0xFFEA580C), "S3")
                    "sftp" -> Triple(Color(0xFFEFF6FF), Color(0xFF2563EB), "SFTP")
                    "webdav" -> Triple(Color(0xFFF0FDF4), Color(0xFF059669), "WebDAV")
                    else -> Triple(Color(0xFFF3F4F6), Color(0xFF71717A), "本地")
                }
                Box(modifier = Modifier.background(bg, RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                    Text(label, fontSize = 10.sp, color = tc)
                }
            }
            val endpoint = when (storage.type) {
                "s3" -> storage.info.endpoint.ifEmpty { storage.info.bucket }
                "sftp" -> "${storage.info.username.ifEmpty { "?" }}@${storage.info.host}:${storage.info.port.let { if (it > 0) it else 22 }}"
                "webdav" -> storage.info.url
                else -> "本地存储"
            }
            if (endpoint.isNotEmpty()) Text(endpoint, fontSize = 12.sp, color = Color(0xFF71717A))
        }

        if (isActing) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color(0xFF2563EB))
        } else if (!isLocal) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
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
private fun CreateStorageDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String, BackupStorageInfo) -> Unit
) {
    var type by remember { mutableStateOf("s3") }
    var name by remember { mutableStateOf("") }
    // s3
    var s3AccessKey by remember { mutableStateOf("") }
    var s3SecretKey by remember { mutableStateOf("") }
    var s3Endpoint by remember { mutableStateOf("") }
    var s3Bucket by remember { mutableStateOf("") }
    // sftp
    var sftpHost by remember { mutableStateOf("") }
    var sftpPort by remember { mutableStateOf("22") }
    var sftpUser by remember { mutableStateOf("") }
    var sftpPassword by remember { mutableStateOf("") }
    // webdav
    var webdavUrl by remember { mutableStateOf("") }
    var webdavUser by remember { mutableStateOf("") }
    var webdavPassword by remember { mutableStateOf("") }

    val typeItems = listOf("s3" to "S3", "sftp" to "SFTP", "webdav" to "WebDAV")

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                .background(Color.White).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("新建备份存储", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF18181B))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                typeItems.forEach { (key, label) ->
                    Box(
                        modifier = Modifier.height(32.dp)
                            .background(if (type == key) Color(0xFF2563EB) else Color(0xFFF3F4F6), RoundedCornerShape(8.dp))
                            .clickable { type = key }
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center
                    ) { Text(label, fontSize = 13.sp, color = if (type == key) Color.White else Color(0xFF71717A)) }
                }
            }

            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("名称") },
                modifier = Modifier.fillMaxWidth(), singleLine = true)

            when (type) {
                "s3" -> {
                    OutlinedTextField(value = s3Endpoint, onValueChange = { s3Endpoint = it }, label = { Text("Endpoint") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = s3Bucket, onValueChange = { s3Bucket = it }, label = { Text("Bucket") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = s3AccessKey, onValueChange = { s3AccessKey = it }, label = { Text("Access Key") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = s3SecretKey, onValueChange = { s3SecretKey = it }, label = { Text("Secret Key") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true)
                }
                "sftp" -> {
                    OutlinedTextField(value = sftpHost, onValueChange = { sftpHost = it }, label = { Text("主机") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = sftpPort, onValueChange = { sftpPort = it }, label = { Text("端口") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = sftpUser, onValueChange = { sftpUser = it }, label = { Text("用户名") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = sftpPassword, onValueChange = { sftpPassword = it }, label = { Text("密码") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true)
                }
                "webdav" -> {
                    OutlinedTextField(value = webdavUrl, onValueChange = { webdavUrl = it }, label = { Text("WebDAV URL") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = webdavUser, onValueChange = { webdavUser = it }, label = { Text("用户名") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = webdavPassword, onValueChange = { webdavPassword = it }, label = { Text("密码") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true)
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("取消", color = Color(0xFF71717A)) }
                Spacer(Modifier.width(8.dp))
                val canSubmit = name.isNotBlank() && when (type) {
                    "s3" -> s3Endpoint.isNotBlank() && s3Bucket.isNotBlank()
                    "sftp" -> sftpHost.isNotBlank() && sftpUser.isNotBlank()
                    "webdav" -> webdavUrl.isNotBlank()
                    else -> false
                }
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(8.dp))
                        .background(if (canSubmit) Color(0xFF2563EB) else Color(0xFF71717A))
                        .clickable(enabled = canSubmit) {
                            val info = when (type) {
                                "s3" -> BackupStorageInfo(access_key = s3AccessKey, secret_key = s3SecretKey,
                                    endpoint = s3Endpoint, bucket = s3Bucket, scheme = "https", style = "virtual-hosted")
                                "sftp" -> BackupStorageInfo(host = sftpHost,
                                    port = sftpPort.toIntOrNull() ?: 22, username = sftpUser, password = sftpPassword)
                                else -> BackupStorageInfo(url = webdavUrl, username = webdavUser, password = webdavPassword)
                            }
                            onCreate(type, name.trim(), info)
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) { Text("创建", fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Medium) }
            }
        }
    }
}

@Composable
private fun EditStorageDialog(
    storage: BackupStorageItem,
    actionError: String?,
    onDismiss: () -> Unit,
    onUpdate: (String, String, BackupStorageInfo) -> Unit
) {
    var name by remember { mutableStateOf(storage.name) }
    val type = storage.type  // type is fixed on edit
    // s3
    var s3AccessKey by remember { mutableStateOf(storage.info.access_key) }
    var s3SecretKey by remember { mutableStateOf("") }  // secret key not pre-filled for security
    var s3Endpoint by remember { mutableStateOf(storage.info.endpoint) }
    var s3Bucket by remember { mutableStateOf(storage.info.bucket) }
    // sftp
    var sftpHost by remember { mutableStateOf(storage.info.host) }
    var sftpPort by remember { mutableStateOf(storage.info.port.let { if (it > 0) it.toString() else "22" }) }
    var sftpUser by remember { mutableStateOf(storage.info.username) }
    var sftpPassword by remember { mutableStateOf("") }  // not pre-filled for security
    // webdav
    var webdavUrl by remember { mutableStateOf(storage.info.url) }
    var webdavUser by remember { mutableStateOf(storage.info.username) }
    var webdavPassword by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                .background(Color.White).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("编辑备份存储", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF18181B))

            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("名称") },
                modifier = Modifier.fillMaxWidth(), singleLine = true)

            when (type) {
                "s3" -> {
                    OutlinedTextField(value = s3Endpoint, onValueChange = { s3Endpoint = it }, label = { Text("Endpoint") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = s3Bucket, onValueChange = { s3Bucket = it }, label = { Text("Bucket") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = s3AccessKey, onValueChange = { s3AccessKey = it }, label = { Text("Access Key") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = s3SecretKey, onValueChange = { s3SecretKey = it }, label = { Text("Secret Key（留空不修改）") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true)
                }
                "sftp" -> {
                    OutlinedTextField(value = sftpHost, onValueChange = { sftpHost = it }, label = { Text("主机") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = sftpPort, onValueChange = { sftpPort = it }, label = { Text("端口") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = sftpUser, onValueChange = { sftpUser = it }, label = { Text("用户名") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = sftpPassword, onValueChange = { sftpPassword = it }, label = { Text("密码（留空不修改）") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true)
                }
                "webdav" -> {
                    OutlinedTextField(value = webdavUrl, onValueChange = { webdavUrl = it }, label = { Text("WebDAV URL") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = webdavUser, onValueChange = { webdavUser = it }, label = { Text("用户名") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = webdavPassword, onValueChange = { webdavPassword = it }, label = { Text("密码（留空不修改）") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true)
                }
            }

            if (actionError != null) {
                Text(actionError, fontSize = 12.sp, color = Color(0xFFEF4444))
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("取消", color = Color(0xFF71717A)) }
                Spacer(Modifier.width(8.dp))
                val canSubmit = name.isNotBlank() && when (type) {
                    "s3" -> s3Endpoint.isNotBlank() && s3Bucket.isNotBlank()
                    "sftp" -> sftpHost.isNotBlank() && sftpUser.isNotBlank()
                    "webdav" -> webdavUrl.isNotBlank()
                    else -> false
                }
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(8.dp))
                        .background(if (canSubmit) Color(0xFF2563EB) else Color(0xFF71717A))
                        .clickable(enabled = canSubmit) {
                            val info = when (type) {
                                "s3" -> BackupStorageInfo(access_key = s3AccessKey,
                                    secret_key = s3SecretKey.ifBlank { storage.info.secret_key },
                                    endpoint = s3Endpoint, bucket = s3Bucket,
                                    scheme = storage.info.scheme.ifEmpty { "https" },
                                    style = storage.info.style.ifEmpty { "virtual-hosted" })
                                "sftp" -> BackupStorageInfo(host = sftpHost,
                                    port = sftpPort.toIntOrNull() ?: 22, username = sftpUser,
                                    password = sftpPassword.ifBlank { storage.info.password })
                                else -> BackupStorageInfo(url = webdavUrl, username = webdavUser,
                                    password = webdavPassword.ifBlank { storage.info.password })
                            }
                            onUpdate(type, name.trim(), info)
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) { Text("保存", fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Medium) }
            }
        }
    }
}
