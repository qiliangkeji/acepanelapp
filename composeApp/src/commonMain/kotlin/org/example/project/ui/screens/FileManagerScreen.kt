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
import org.example.project.data.FileListItem
import org.example.project.ui.components.StatusBarSpacer
import org.example.project.viewmodel.FileManagerViewModel

@Composable
fun FileManagerScreen(
    panelId: String = "",
    initialPath: String = "/",
    onBackClick: () -> Unit = {},
    onAddClick: () -> Unit = {},
    onOpenFile: ((String) -> Unit)? = null
) {
    val vm: FileManagerViewModel = viewModel()
    val files by vm.files.collectAsStateWithLifecycle()
    val currentPath by vm.currentPath.collectAsStateWithLifecycle()
    val isLoading by vm.isLoading.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    val actionError by vm.actionError.collectAsStateWithLifecycle()
    val showCreateDialog by vm.showCreateDialog.collectAsStateWithLifecycle()

    LaunchedEffect(panelId) {
        if (panelId.isNotEmpty()) vm.init(panelId, initialPath)
    }

    if (showCreateDialog) {
        CreateFileDialog(
            onDismiss = { vm.hideCreate() },
            onCreate = { name, isDir -> vm.createItem(name, isDir) }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        StatusBarSpacer()
        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.5.dp, Color(0xFFE4E4E7), RoundedCornerShape(10.dp))
                        .clickable {
                            if (!vm.goBack()) onBackClick()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "‹", fontSize = 20.sp, color = Color(0xFF18181B))
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "文件管理",
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
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.5.dp, Color(0xFFE4E4E7), RoundedCornerShape(10.dp))
                            .clickable { vm.load() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "↻", fontSize = 18.sp, color = Color(0xFF18181B))
                    }
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF2563EB))
                            .clickable { vm.showCreate() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "+", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }

            // 当前路径
            Text(
                text = currentPath,
                fontSize = 12.sp,
                color = Color(0xFF71717A),
                maxLines = 1
            )
        }

        // 错误提示
        if (error != null) {
            Text(
                text = "加载失败: $error",
                fontSize = 12.sp,
                color = Color(0xFFEF4444),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 2.dp)
            )
        }
        if (actionError != null) {
            Text(
                text = actionError!!,
                fontSize = 12.sp,
                color = Color(0xFFEF4444),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 2.dp)
            )
        }

        if (files.isEmpty() && !isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = if (panelId.isEmpty()) "请先选择面板" else "目录为空",
                    fontSize = 14.sp, color = Color(0xFFA1A1AA)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                items(files) { file ->
                    FileItemRow(
                        file = file,
                        onClick = {
                            if (file.dir) {
                                vm.navigate(file.full)
                            } else {
                                onOpenFile?.invoke(file.full)
                            }
                        },
                        onDelete = { vm.deleteFile(file.full) }
                    )
                    HorizontalDivider(thickness = 0.5.dp, color = Color(0xFFF0F0F0))
                }
            }
        }
    }
}

@Composable
private fun FileItemRow(
    file: FileListItem,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        Dialog(onDismissRequest = { showDeleteConfirm = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("确认删除", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF18181B))
                Text("删除 \"${file.name}\"？此操作不可恢复。", fontSize = 14.sp, color = Color(0xFF71717A))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { showDeleteConfirm = false }) {
                        Text("取消", color = Color(0xFF71717A))
                    }
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFEF4444))
                            .clickable { showDeleteConfirm = false; onDelete() }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text("删除", fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = when {
                    file.dir -> "📁"
                    file.name.endsWith(".php") || file.name.endsWith(".html") || file.name.endsWith(".htm") -> "🌐"
                    file.name.endsWith(".conf") || file.name.endsWith(".ini") || file.name.endsWith(".env") -> "⚙️"
                    file.name.endsWith(".log") -> "📋"
                    file.name.endsWith(".zip") || file.name.endsWith(".gz") || file.name.endsWith(".tar") -> "🗜️"
                    file.name.endsWith(".jpg") || file.name.endsWith(".png") || file.name.endsWith(".gif") -> "🖼️"
                    else -> "📄"
                },
                fontSize = 18.sp
            )

            Column {
                Text(
                    text = file.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (file.hidden) Color(0xFFA1A1AA) else Color(0xFF18181B),
                    maxLines = 1
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (file.size.isNotEmpty()) {
                        Text(text = file.size, fontSize = 11.sp, color = Color(0xFF71717A))
                    }
                    if (file.modify.isNotEmpty()) {
                        Text(text = file.modify.take(10), fontSize = 11.sp, color = Color(0xFFA1A1AA))
                    }
                }
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 删除按钮
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFFFEF2F2))
                    .clickable { showDeleteConfirm = true },
                contentAlignment = Alignment.Center
            ) {
                Text("✕", fontSize = 11.sp, color = Color(0xFFEF4444))
            }

            if (file.dir) {
                Text(text = "›", fontSize = 20.sp, color = Color(0xFFA1A1AA))
            }
        }
    }
}

@Composable
private fun CreateFileDialog(
    onDismiss: () -> Unit,
    onCreate: (String, Boolean) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var isDir by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("新建", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF18181B))

            // 类型选择
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("文件" to false, "目录" to true).forEach { (label, isDirectory) ->
                    Box(
                        modifier = Modifier
                            .height(32.dp)
                            .background(
                                if (isDir == isDirectory) Color(0xFF2563EB) else Color(0xFFF3F4F6),
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { isDir = isDirectory }
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 13.sp,
                            color = if (isDir == isDirectory) Color.White else Color(0xFF71717A),
                            fontWeight = if (isDir == isDirectory) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("名称") },
                placeholder = { Text(if (isDir) "目录名" else "文件名") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) {
                    Text("取消", color = Color(0xFF71717A))
                }
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (name.isNotBlank()) Color(0xFF2563EB) else Color(0xFF71717A))
                        .clickable(enabled = name.isNotBlank()) {
                            onCreate(name.trim(), isDir)
                            onDismiss()
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text("创建", fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

// 保留兼容旧调用的数据类
data class FileItem(
    val name: String,
    val type: String,
    val size: String,
    val isFolder: Boolean
)
