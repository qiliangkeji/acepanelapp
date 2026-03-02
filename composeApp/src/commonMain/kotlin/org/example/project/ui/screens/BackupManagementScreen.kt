package org.example.project.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
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
import org.example.project.data.BackupFileItem
import org.example.project.ui.components.StatusBarSpacer
import org.example.project.viewmodel.BackupViewModel

@Composable
fun BackupManagementScreen(
    panelId: String = "",
    onBack: () -> Unit,
    onAddClick: () -> Unit = {}
) {
    val vm: BackupViewModel = viewModel()
    val backups by vm.backups.collectAsStateWithLifecycle()
    val backupType by vm.backupType.collectAsStateWithLifecycle()
    val isLoading by vm.isLoading.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    val actionFile by vm.actionFile.collectAsStateWithLifecycle()
    val actionError by vm.actionError.collectAsStateWithLifecycle()
    val showCreateDialog by vm.showCreateDialog.collectAsStateWithLifecycle()
    val isCreating by vm.isCreating.collectAsStateWithLifecycle()
    val showRestoreDialog by vm.showRestoreDialog.collectAsStateWithLifecycle()
    val isRestoring by vm.isRestoring.collectAsStateWithLifecycle()

    LaunchedEffect(panelId) {
        if (panelId.isNotEmpty()) vm.init(panelId)
    }

    if (showCreateDialog) {
        CreateBackupDialog(
            backupType = backupType,
            isCreating = isCreating,
            actionError = actionError,
            onDismiss = { vm.hideCreate() },
            onCreate = { target -> vm.createBackup(target) }
        )
    }

    showRestoreDialog?.let { fileName ->
        RestoreBackupDialog(
            backupType = backupType,
            fileName = fileName,
            isRestoring = isRestoring,
            actionError = actionError,
            onDismiss = { vm.hideRestore() },
            onRestore = { target -> vm.restoreBackup(fileName, target) }
        )
    }

    val typeItems = listOf(
        "website" to "网站",
        "mysql" to "MySQL",
        "postgresql" to "PostgreSQL",
        "panel" to "面板"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        StatusBarSpacer()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .border(1.5.dp, Color(0xFFE4E4E7), RoundedCornerShape(10.dp))
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "←", fontSize = 18.sp, color = Color(0xFF18181B))
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "备份管理",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF18181B)
                )
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color(0xFF2563EB))
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .border(1.5.dp, Color(0xFFE4E4E7), RoundedCornerShape(10.dp))
                        .clickable { vm.load() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "↻", fontSize = 16.sp, color = Color(0xFF18181B))
                }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFF2563EB), RoundedCornerShape(10.dp))
                        .clickable { vm.showCreate() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "+", fontSize = 20.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        // 类型选择 Tab
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(typeItems) { (key, label) ->
                val isSelected = backupType == key
                Box(
                    modifier = Modifier
                        .height(32.dp)
                        .background(
                            if (isSelected) Color(0xFF2563EB) else Color(0xFFF3F4F6),
                            RoundedCornerShape(16.dp)
                        )
                        .clickable { vm.loadByType(key) }
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (isSelected) Color.White else Color(0xFF71717A)
                    )
                }
            }
        }

        // 统计卡
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(72.dp)
                    .background(Color(0xFFEFF6FF), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "${backups.size}",
                    fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2563EB)
                )
                Text(text = "备份数量", fontSize = 12.sp, color = Color(0xFF2563EB))
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(72.dp)
                    .background(Color(0xFFF3F4F6), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val typeName = typeItems.find { it.first == backupType }?.second ?: backupType
                Text(text = typeName, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF18181B))
                Text(text = "当前类型", fontSize = 12.sp, color = Color(0xFF71717A))
            }
        }

        if (error != null) {
            Text(
                text = "加载失败: $error",
                fontSize = 12.sp, color = Color(0xFFEF4444),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
            )
        }
        if (actionError != null) {
            Text(
                text = actionError!!,
                fontSize = 12.sp, color = Color(0xFFEF4444),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
            )
        }

        Text(
            text = "备份列表",
            fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF71717A), letterSpacing = 1.sp,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )

        if (backups.isEmpty() && !isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = if (panelId.isEmpty()) "请先选择面板" else "暂无备份",
                    fontSize = 14.sp, color = Color(0xFFA1A1AA)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(backups) { backup ->
                    BackupCard(
                        backup = backup,
                        isActing = actionFile == backup.name,
                        onDelete = { vm.deleteBackup(backup.name) },
                        onRestore = { vm.showRestore(backup.name) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CreateBackupDialog(
    backupType: String,
    isCreating: Boolean,
    actionError: String?,
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {
    var target by remember { mutableStateOf("") }
    val typeLabels = mapOf(
        "website" to "网站名称",
        "mysql" to "数据库名称",
        "postgresql" to "数据库名称",
        "panel" to "（留空备份整个面板）"
    )
    val isPanel = backupType == "panel"

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("立即备份", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF18181B))

            val labelMap = mapOf(
                "website" to "网站", "mysql" to "MySQL", "postgresql" to "PostgreSQL", "panel" to "面板"
            )
            Text("备份类型：${labelMap[backupType] ?: backupType}", fontSize = 13.sp, color = Color(0xFF71717A))

            if (!isPanel) {
                OutlinedTextField(
                    value = target,
                    onValueChange = { target = it },
                    label = { Text(typeLabels[backupType] ?: "目标名称") },
                    placeholder = { Text("例如: my_website") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Text("仅允许字母、数字、下划线、连字符", fontSize = 11.sp, color = Color(0xFFA1A1AA))
            }

            if (actionError != null) {
                Text(actionError, fontSize = 12.sp, color = Color(0xFFEF4444))
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("取消", color = Color(0xFF71717A)) }
                Spacer(Modifier.width(8.dp))
                val canSubmit = !isCreating && (isPanel || target.isNotBlank())
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (canSubmit) Color(0xFF2563EB) else Color(0xFF71717A))
                        .clickable(enabled = canSubmit) { onCreate(target.trim()) }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    if (isCreating) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                    } else {
                        Text("开始备份", fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

@Composable
private fun BackupCard(
    backup: BackupFileItem,
    isActing: Boolean,
    onDelete: () -> Unit,
    onRestore: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, Color(0xFFE4E4E7), RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = backup.name,
                    fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF18181B), maxLines = 2
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (backup.size.isNotEmpty()) {
                        Text(text = backup.size, fontSize = 12.sp, color = Color(0xFF71717A))
                    }
                    if (backup.time.isNotEmpty()) {
                        Text(text = backup.time.take(10), fontSize = 12.sp, color = Color(0xFFA1A1AA))
                    }
                }
            }

            if (isActing) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color(0xFF2563EB))
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .height(28.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFEFF6FF))
                            .clickable(onClick = onRestore)
                            .padding(horizontal = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("恢复", fontSize = 11.sp, color = Color(0xFF2563EB), fontWeight = FontWeight.Medium)
                    }
                    Box(
                        modifier = Modifier
                            .height(28.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .border(1.dp, Color(0xFFEF4444), RoundedCornerShape(6.dp))
                            .clickable(onClick = onDelete)
                            .padding(horizontal = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("删除", fontSize = 11.sp, color = Color(0xFFEF4444), fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

@Composable
private fun RestoreBackupDialog(
    backupType: String,
    fileName: String,
    isRestoring: Boolean,
    actionError: String?,
    onDismiss: () -> Unit,
    onRestore: (String) -> Unit
) {
    var target by remember { mutableStateOf("") }
    val isPanel = backupType == "panel"
    val typeLabels = mapOf("website" to "恢复网站名称", "mysql" to "恢复数据库名", "postgresql" to "恢复数据库名")

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("恢复备份", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF18181B))
            Text("文件：${fileName.takeLast(40)}", fontSize = 12.sp, color = Color(0xFF71717A))

            if (!isPanel) {
                OutlinedTextField(
                    value = target, onValueChange = { target = it },
                    label = { Text(typeLabels[backupType] ?: "恢复目标") },
                    placeholder = { Text("例如: my_website") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true
                )
                Text("仅允许字母、数字、下划线、连字符", fontSize = 11.sp, color = Color(0xFFA1A1AA))
            }

            if (actionError != null) Text(actionError, fontSize = 12.sp, color = Color(0xFFEF4444))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("取消", color = Color(0xFF71717A)) }
                Spacer(Modifier.width(8.dp))
                val canSubmit = !isRestoring && (isPanel || target.isNotBlank())
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(8.dp))
                        .background(if (canSubmit) Color(0xFF2563EB) else Color(0xFF71717A))
                        .clickable(enabled = canSubmit) { onRestore(target.trim()) }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    if (isRestoring) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                    } else {
                        Text("开始恢复", fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}
