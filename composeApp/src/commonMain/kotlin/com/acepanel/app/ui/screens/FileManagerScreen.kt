package com.acepanel.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import com.acepanel.app.data.FileListItem
import com.acepanel.app.ui.components.AnimatedAppDialog
import com.acepanel.app.ui.components.StatusBarSpacer
import com.acepanel.app.viewmodel.FileClipboardMode
import com.acepanel.app.viewmodel.FileManagerViewModel

@Composable
fun FileManagerScreen(
    panelId: String = "",
    initialPath: String = "/",
    initialFirstVisibleItemIndex: Int = 0,
    initialFirstVisibleItemScrollOffset: Int = 0,
    restoreAnchorPath: String? = null,
    onBackClick: (Int, Int) -> Unit = { _, _ -> },
    onAddClick: () -> Unit = {},
    onOpenFile: ((String, Int, Int) -> Unit)? = null,
    onOpenDirectory: (String, Int, Int) -> Unit = { _, _, _ -> }
) {
    val vm: FileManagerViewModel = viewModel()
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = initialFirstVisibleItemIndex,
        initialFirstVisibleItemScrollOffset = initialFirstVisibleItemScrollOffset
    )
    val files by vm.files.collectAsStateWithLifecycle()
    val currentPath by vm.currentPath.collectAsStateWithLifecycle()
    val isLoading by vm.isLoading.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    val actionError by vm.actionError.collectAsStateWithLifecycle()
    val showCreateDialog by vm.showCreateDialog.collectAsStateWithLifecycle()
    val clipboard by vm.clipboard.collectAsStateWithLifecycle()
    var hasRestoredPosition by remember(initialPath, restoreAnchorPath) { mutableStateOf(false) }
    var showRemoteDownloadDialog by remember { mutableStateOf(false) }

    LaunchedEffect(panelId, initialPath) {
        if (panelId.isNotEmpty()) vm.init(panelId, initialPath)
    }

    LaunchedEffect(currentPath, isLoading, files, restoreAnchorPath) {
        if (hasRestoredPosition || currentPath != initialPath || isLoading || files.isEmpty()) return@LaunchedEffect

        val anchorIndex = restoreAnchorPath?.let { anchor ->
            files.indexOfFirst { it.full == anchor }.takeIf { it >= 0 }
        }

        when {
            anchorIndex != null -> listState.scrollToItem(anchorIndex)
            initialFirstVisibleItemIndex > 0 || initialFirstVisibleItemScrollOffset > 0 -> {
                val targetIndex = initialFirstVisibleItemIndex.coerceAtMost(files.lastIndex)
                listState.scrollToItem(targetIndex, initialFirstVisibleItemScrollOffset)
            }
        }
        hasRestoredPosition = true
    }

    if (showCreateDialog) {
        CreateFileDialog(
            onDismiss = { vm.hideCreate() },
            onCreate = { name, isDir -> vm.createItem(name, isDir) }
        )
    }

    if (showRemoteDownloadDialog) {
        RemoteDownloadDialog(
            onDismiss = { showRemoteDownloadDialog = false },
            onDownload = { url, fileName ->
                vm.remoteDownload(url, fileName)
                showRemoteDownloadDialog = false
            }
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
                            onBackClick(
                                listState.firstVisibleItemIndex,
                                listState.firstVisibleItemScrollOffset
                            )
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
                    if (clipboard != null) {
                        Box(
                            modifier = Modifier
                                .height(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .border(1.5.dp, Color(0xFFE4E4E7), RoundedCornerShape(10.dp))
                                .clickable { vm.pasteMarked() }
                                .padding(horizontal = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (clipboard?.mode == FileClipboardMode.Copy) "粘贴" else "移动到此",
                                fontSize = 12.sp,
                                color = Color(0xFF18181B),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.5.dp, Color(0xFFE4E4E7), RoundedCornerShape(10.dp))
                            .clickable { showRemoteDownloadDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "↓", fontSize = 18.sp, color = Color(0xFF18181B))
                    }
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
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                items(files, key = { it.full }) { file ->
                    FileItemRow(
                        file = file,
                        onClick = {
                            if (file.dir) {
                                onOpenDirectory(
                                    file.full,
                                    listState.firstVisibleItemIndex,
                                    listState.firstVisibleItemScrollOffset
                                )
                            } else {
                                onOpenFile?.invoke(
                                    file.full,
                                    listState.firstVisibleItemIndex,
                                    listState.firstVisibleItemScrollOffset
                                )
                            }
                        },
                        onDelete = { vm.deleteFile(file.full) },
                        onRename = { newName -> vm.renameFile(file, newName) },
                        onCopy = { vm.markForCopy(file) },
                        onMove = { vm.markForMove(file) },
                        onPermission = { mode, owner, group -> vm.updatePermission(file, mode, owner, group) },
                        onCompress = { archiveName -> vm.compress(file, archiveName) },
                        onUnCompress = { targetPath -> vm.unCompress(file, targetPath) }
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
    onDelete: () -> Unit,
    onRename: (String) -> Unit,
    onCopy: () -> Unit,
    onMove: () -> Unit,
    onPermission: (String, String, String) -> Unit,
    onCompress: (String) -> Unit,
    onUnCompress: (String) -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showActions by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    var showCompressDialog by remember { mutableStateOf(false) }
    var showUnCompressDialog by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AnimatedAppDialog(onDismissRequest = { showDeleteConfirm = false }) {
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

    if (showRenameDialog) {
        SingleTextInputDialog(
            title = "重命名",
            label = "名称",
            initial = file.name,
            onDismiss = { showRenameDialog = false },
            onConfirm = {
                onRename(it)
                showRenameDialog = false
            }
        )
    }

    if (showPermissionDialog) {
        PermissionDialog(
            file = file,
            onDismiss = { showPermissionDialog = false },
            onConfirm = { mode, owner, group ->
                onPermission(mode, owner, group)
                showPermissionDialog = false
            }
        )
    }

    if (showCompressDialog) {
        SingleTextInputDialog(
            title = "压缩",
            label = "压缩包名称",
            initial = "${file.name}.zip",
            onDismiss = { showCompressDialog = false },
            onConfirm = {
                onCompress(it)
                showCompressDialog = false
            }
        )
    }

    if (showUnCompressDialog) {
        SingleTextInputDialog(
            title = "解压",
            label = "解压到",
            initial = file.full.substringBeforeLast('/', missingDelimiterValue = "/").ifEmpty { "/" },
            onDismiss = { showUnCompressDialog = false },
            onConfirm = {
                onUnCompress(it)
                showUnCompressDialog = false
            }
        )
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
            Box {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFF3F4F6))
                        .clickable { showActions = true },
                    contentAlignment = Alignment.Center
                ) {
                    Text("⋯", fontSize = 16.sp, color = Color(0xFF18181B))
                }
                DropdownMenu(expanded = showActions, onDismissRequest = { showActions = false }) {
                    DropdownMenuItem(
                        text = { Text("重命名") },
                        onClick = { showActions = false; showRenameDialog = true }
                    )
                    DropdownMenuItem(
                        text = { Text("复制") },
                        onClick = { showActions = false; onCopy() }
                    )
                    DropdownMenuItem(
                        text = { Text("移动") },
                        onClick = { showActions = false; onMove() }
                    )
                    DropdownMenuItem(
                        text = { Text("权限") },
                        onClick = { showActions = false; showPermissionDialog = true }
                    )
                    DropdownMenuItem(
                        text = { Text("压缩") },
                        onClick = { showActions = false; showCompressDialog = true }
                    )
                    if (file.isCompressArchive()) {
                        DropdownMenuItem(
                            text = { Text("解压") },
                            onClick = { showActions = false; showUnCompressDialog = true }
                        )
                    }
                }
            }

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

    AnimatedAppDialog(onDismissRequest = onDismiss) {
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

@Composable
private fun SingleTextInputDialog(
    title: String,
    label: String,
    initial: String = "",
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var value by remember(initial) { mutableStateOf(initial) }

    AnimatedAppDialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(title, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF18181B))
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                label = { Text(label) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("取消", color = Color(0xFF71717A)) }
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (value.isNotBlank()) Color(0xFF2563EB) else Color(0xFF71717A))
                        .clickable(enabled = value.isNotBlank()) { onConfirm(value.trim()) }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text("确定", fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun PermissionDialog(
    file: FileListItem,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit
) {
    var mode by remember(file.full) { mutableStateOf(file.mode.ifBlank { "0755" }) }
    var owner by remember(file.full) { mutableStateOf(file.owner.ifBlank { "www" }) }
    var group by remember(file.full) { mutableStateOf(file.group.ifBlank { "www" }) }

    AnimatedAppDialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("修改权限", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF18181B))
            Text(file.full, fontSize = 12.sp, color = Color(0xFF71717A), maxLines = 2)
            OutlinedTextField(
                value = mode,
                onValueChange = { mode = it.filter { ch -> ch in '0'..'7' }.take(4) },
                label = { Text("权限") },
                placeholder = { Text("0755") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = owner,
                    onValueChange = { owner = it },
                    label = { Text("所有者") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = group,
                    onValueChange = { group = it },
                    label = { Text("用户组") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("取消", color = Color(0xFF71717A)) }
                Spacer(Modifier.width(8.dp))
                val canSubmit = mode.isNotBlank() && owner.isNotBlank() && group.isNotBlank()
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (canSubmit) Color(0xFF2563EB) else Color(0xFF71717A))
                        .clickable(enabled = canSubmit) { onConfirm(mode, owner, group) }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text("保存", fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun RemoteDownloadDialog(
    onDismiss: () -> Unit,
    onDownload: (String, String) -> Unit
) {
    var url by remember { mutableStateOf("") }
    var fileName by remember { mutableStateOf("") }

    LaunchedEffect(url) {
        if (fileName.isBlank()) {
            fileName = url.substringBefore('?').substringAfterLast('/').takeIf { it.isNotBlank() } ?: ""
        }
    }

    AnimatedAppDialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("远程下载", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF18181B))
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text("URL") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = fileName,
                onValueChange = { fileName = it },
                label = { Text("保存为") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("取消", color = Color(0xFF71717A)) }
                Spacer(Modifier.width(8.dp))
                val canSubmit = url.isNotBlank() && fileName.isNotBlank()
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (canSubmit) Color(0xFF2563EB) else Color(0xFF71717A))
                        .clickable(enabled = canSubmit) { onDownload(url.trim(), fileName.trim()) }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text("开始", fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

private fun FileListItem.isCompressArchive(): Boolean {
    val lower = name.lowercase()
    return lower.endsWith(".zip") ||
        lower.endsWith(".tar") ||
        lower.endsWith(".tar.gz") ||
        lower.endsWith(".tgz") ||
        lower.endsWith(".gz") ||
        lower.endsWith(".7z") ||
        lower.endsWith(".rar")
}

// 保留兼容旧调用的数据类
data class FileItem(
    val name: String,
    val type: String,
    val size: String,
    val isFolder: Boolean
)
