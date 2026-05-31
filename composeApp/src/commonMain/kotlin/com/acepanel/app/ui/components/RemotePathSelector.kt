package com.acepanel.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.acepanel.app.data.FileListItem
import com.acepanel.app.data.PanelRepository
import com.acepanel.app.network.PanelApiService

enum class RemotePathMode {
    Directory,
    File,
    Any
}

@Composable
fun RemotePathSelector(
    panelId: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String = "请选择路径",
    mode: RemotePathMode = RemotePathMode.Any,
    allowClear: Boolean = true,
    enabled: Boolean = true
) {
    var showPicker by remember { mutableStateOf(false) }

    if (showPicker) {
        RemotePathPickerDialog(
            panelId = panelId,
            initialPath = pickerInitialPath(value, mode),
            mode = mode,
            onDismiss = { showPicker = false },
            onSelect = {
                onValueChange(it)
                showPicker = false
            }
        )
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (label != null) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF71717A),
                letterSpacing = 1.sp
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f)) {
                OutlinedTextField(
                    value = value,
                    onValueChange = {},
                    readOnly = true,
                    enabled = enabled,
                    placeholder = { Text(placeholder, color = Color(0xFFA1A1AA), fontSize = 14.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF2563EB),
                        unfocusedBorderColor = Color(0xFFE4E4E7),
                        disabledBorderColor = Color(0xFFE4E4E7),
                        focusedTextColor = Color(0xFF18181B),
                        unfocusedTextColor = Color(0xFF18181B),
                        disabledTextColor = Color(0xFF18181B)
                    )
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable(enabled = enabled && panelId.isNotBlank()) { showPicker = true }
                )
            }
            if (allowClear && value.isNotBlank()) {
                PathButton("清空", enabled = enabled) { onValueChange("") }
            }
            PathButton("选择", enabled = enabled && panelId.isNotBlank()) { showPicker = true }
        }
    }
}

@Composable
private fun PathButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (enabled) Color(0xFF2563EB) else Color(0xFFA1A1AA))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun RemotePathPickerDialog(
    panelId: String,
    initialPath: String,
    mode: RemotePathMode,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    val service = remember(panelId) {
        PanelRepository.getPanel(panelId)?.let { PanelApiService(it) }
    }
    var currentPath by remember(initialPath) { mutableStateOf(normalizePath(initialPath)) }
    var files by remember { mutableStateOf<List<FileListItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    DisposableEffect(service) {
        onDispose { service?.close() }
    }

    LaunchedEffect(service, currentPath) {
        if (service == null) {
            error = "面板配置不存在"
            return@LaunchedEffect
        }
        isLoading = true
        error = null
        service.getFileList(currentPath, page = 1, limit = 300)
            .onSuccess { files = it.items.sortedWith(compareByDescending<FileListItem> { item -> item.dir }.thenBy { item -> item.name.lowercase() }) }
            .onFailure { error = it.message ?: "加载失败" }
        isLoading = false
    }

    AnimatedAppDialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when (mode) {
                        RemotePathMode.Directory -> "选择目录"
                        RemotePathMode.File -> "选择文件"
                        RemotePathMode.Any -> "选择路径"
                    },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF18181B)
                )
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color(0xFF2563EB))
                }
            }

            Text(currentPath, fontSize = 12.sp, color = Color(0xFF71717A), maxLines = 2)

            if (mode != RemotePathMode.File) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFEFF6FF))
                        .clickable { onSelect(currentPath) },
                    contentAlignment = Alignment.Center
                ) {
                    Text("选择当前目录", fontSize = 13.sp, color = Color(0xFF2563EB), fontWeight = FontWeight.Medium)
                }
            }

            if (error != null) {
                Text(error!!, fontSize = 12.sp, color = Color(0xFFEF4444))
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .border(1.dp, Color(0xFFE4E4E7), RoundedCornerShape(12.dp)),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                if (currentPath != "/") {
                    item {
                        PathPickerRow(name = "..", description = "上级目录", isDirectory = true) {
                            currentPath = parentPath(currentPath)
                        }
                        HorizontalDivider(thickness = 0.5.dp, color = Color(0xFFF0F0F0))
                    }
                }
                items(files, key = { it.full }) { file ->
                    val selectable = mode == RemotePathMode.Any ||
                        (mode == RemotePathMode.Directory && file.dir) ||
                        (mode == RemotePathMode.File && !file.dir)
                    PathPickerRow(
                        name = file.name,
                        description = if (file.dir) "目录" else file.size.ifBlank { "文件" },
                        isDirectory = file.dir,
                        selectable = selectable,
                        onSelect = { onSelect(file.full) },
                        onOpen = {
                            if (file.dir) currentPath = normalizePath(file.full)
                            else if (selectable) onSelect(file.full)
                        }
                    )
                    HorizontalDivider(thickness = 0.5.dp, color = Color(0xFFF0F0F0))
                }
                if (!isLoading && files.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(64.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("目录为空", fontSize = 13.sp, color = Color(0xFFA1A1AA))
                        }
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("取消", color = Color(0xFF71717A)) }
            }
        }
    }
}

@Composable
private fun PathPickerRow(
    name: String,
    description: String,
    isDirectory: Boolean,
    selectable: Boolean = false,
    onSelect: () -> Unit = {},
    onOpen: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clickable(onClick = onOpen)
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(if (isDirectory) "📁" else "📄", fontSize = 16.sp)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(name, fontSize = 14.sp, color = Color(0xFF18181B), maxLines = 1)
                Text(description, fontSize = 11.sp, color = Color(0xFFA1A1AA), maxLines = 1)
            }
        }
        if (selectable) {
            Box(
                modifier = Modifier
                    .height(30.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFEFF6FF))
                    .clickable(onClick = onSelect)
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("选择", fontSize = 12.sp, color = Color(0xFF2563EB), fontWeight = FontWeight.Medium)
            }
            Spacer(Modifier.width(4.dp))
        }
        if (isDirectory) {
            Text("›", fontSize = 18.sp, color = Color(0xFFA1A1AA))
        }
    }
}

private fun pickerInitialPath(value: String, mode: RemotePathMode): String {
    val normalized = normalizePath(value)
    return if (mode == RemotePathMode.File && value.isNotBlank()) parentPath(normalized) else normalized
}

private fun normalizePath(path: String): String {
    val trimmed = path.trim().ifBlank { "/" }
    return if (trimmed.startsWith("/")) trimmed else "/$trimmed"
}

private fun parentPath(path: String): String {
    val normalized = normalizePath(path).trimEnd('/').ifEmpty { "/" }
    if (normalized == "/") return "/"
    return normalized.substringBeforeLast('/', missingDelimiterValue = "").ifBlank { "/" }
}
