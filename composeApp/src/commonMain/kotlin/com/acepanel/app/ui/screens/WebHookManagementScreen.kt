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
import com.acepanel.app.data.WebHookItem
import com.acepanel.app.ui.components.AnimatedAppDialog
import com.acepanel.app.ui.components.StatusBarSpacer
import com.acepanel.app.viewmodel.WebHookViewModel

@Composable
fun WebHookManagementScreen(
    panelId: String = "",
    onBack: () -> Unit = {}
) {
    val vm: WebHookViewModel = viewModel()
    val hooks by vm.hooks.collectAsStateWithLifecycle()
    val isLoading by vm.isLoading.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    val actionId by vm.actionId.collectAsStateWithLifecycle()
    val actionError by vm.actionError.collectAsStateWithLifecycle()
    val showCreateDialog by vm.showCreateDialog.collectAsStateWithLifecycle()
    val editHook by vm.showEditDialog.collectAsStateWithLifecycle()

    LaunchedEffect(panelId) {
        if (panelId.isNotEmpty()) vm.init(panelId)
    }

    if (showCreateDialog) {
        CreateWebHookDialog(
            onDismiss = { vm.hideCreate() },
            onCreate = { name, script, user, raw -> vm.createHook(name, script, user, raw) }
        )
    }

    if (editHook != null) {
        EditWebHookDialog(
            hook = editHook!!,
            onDismiss = { vm.hideEdit() },
            onSave = { name, script, user, raw, status ->
                vm.updateHook(editHook!!.id, name, script, user, raw, status)
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
                Text("WebHook", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF18181B))
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

        if (hooks.isEmpty() && !isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(if (panelId.isEmpty()) "请先选择面板" else "暂无 WebHook", fontSize = 14.sp, color = Color(0xFFA1A1AA))
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(hooks) { hook ->
                    WebHookCard(
                        hook = hook,
                        isActing = actionId == hook.id,
                        onEdit = { vm.showEdit(hook) },
                        onToggle = { vm.toggleStatus(hook) },
                        onDelete = { vm.deleteHook(hook.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun WebHookCard(
    hook: WebHookItem,
    isActing: Boolean,
    onEdit: () -> Unit,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
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
                Text(hook.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF18181B))
                Text("执行用户: ${hook.user.ifEmpty { "root" }}", fontSize = 12.sp, color = Color(0xFF71717A))
                if (hook.call_count > 0) {
                    Text("调用次数: ${hook.call_count}", fontSize = 11.sp, color = Color(0xFFA1A1AA))
                }
            }
            if (isActing) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color(0xFF2563EB))
            } else {
                val (statusBg, statusColor, statusLabel) = if (hook.status)
                    Triple(Color(0xFFECFDF5), Color(0xFF059669), "已启用")
                else
                    Triple(Color(0xFFF3F4F6), Color(0xFF71717A), "已停用")
                Box(
                    modifier = Modifier.background(statusBg, RoundedCornerShape(6.dp))
                        .clickable(onClick = onToggle)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) { Text(statusLabel, fontSize = 11.sp, color = statusColor, fontWeight = FontWeight.SemiBold) }
            }
        }

        if (hook.key.isNotEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth()
                    .background(Color(0xFFF3F4F6), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text("/webhook/${hook.key}", fontSize = 11.sp, color = Color(0xFF71717A))
            }
        }

        HorizontalDivider(thickness = 1.dp, color = Color(0xFFE4E4E7))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier.weight(1f).height(32.dp).clip(RoundedCornerShape(8.dp))
                    .border(1.dp, Color(0xFF2563EB), RoundedCornerShape(8.dp))
                    .clickable(enabled = !isActing, onClick = onEdit),
                contentAlignment = Alignment.Center
            ) { Text("编辑", fontSize = 12.sp, color = Color(0xFF2563EB), fontWeight = FontWeight.Medium) }
            Box(
                modifier = Modifier.weight(1f).height(32.dp).clip(RoundedCornerShape(8.dp))
                    .border(1.dp, Color(0xFFEF4444), RoundedCornerShape(8.dp))
                    .clickable(enabled = !isActing, onClick = onDelete),
                contentAlignment = Alignment.Center
            ) { Text("删除", fontSize = 12.sp, color = Color(0xFFEF4444), fontWeight = FontWeight.Medium) }
        }
    }
}

@Composable
private fun CreateWebHookDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String, String, Boolean) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var script by remember { mutableStateOf("") }
    var user by remember { mutableStateOf("root") }
    var raw by remember { mutableStateOf(false) }

    AnimatedAppDialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                .background(Color.White).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("新建 WebHook", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF18181B))
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("名称") },
                modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = script, onValueChange = { script = it }, label = { Text("脚本内容") },
                modifier = Modifier.fillMaxWidth().height(120.dp), maxLines = 6)
            OutlinedTextField(value = user, onValueChange = { user = it }, label = { Text("执行用户") },
                placeholder = { Text("root") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier.size(20.dp)
                        .background(if (raw) Color(0xFF2563EB) else Color(0xFFF3F4F6), RoundedCornerShape(4.dp))
                        .border(1.dp, if (raw) Color(0xFF2563EB) else Color(0xFFD1D5DB), RoundedCornerShape(4.dp))
                        .clickable { raw = !raw },
                    contentAlignment = Alignment.Center
                ) { if (raw) Text("✓", fontSize = 11.sp, color = Color.White) }
                Text("Raw 模式（直接返回文本）", fontSize = 13.sp, color = Color(0xFF71717A))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("取消", color = Color(0xFF71717A)) }
                Spacer(Modifier.width(8.dp))
                val canSubmit = name.isNotBlank() && script.isNotBlank()
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(8.dp))
                        .background(if (canSubmit) Color(0xFF2563EB) else Color(0xFF71717A))
                        .clickable(enabled = canSubmit) { onCreate(name.trim(), script, user.trim().ifEmpty { "root" }, raw) }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) { Text("创建", fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Medium) }
            }
        }
    }
}

@Composable
private fun EditWebHookDialog(
    hook: WebHookItem,
    onDismiss: () -> Unit,
    onSave: (String, String, String, Boolean, Boolean) -> Unit
) {
    var name by remember(hook.id) { mutableStateOf(hook.name) }
    var script by remember(hook.id) { mutableStateOf(hook.script) }
    var user by remember(hook.id) { mutableStateOf(hook.user.ifEmpty { "root" }) }
    var raw by remember(hook.id) { mutableStateOf(hook.raw) }
    var status by remember(hook.id) { mutableStateOf(hook.status) }

    AnimatedAppDialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                .background(Color.White).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("编辑 WebHook", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF18181B))
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("名称") },
                modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = script, onValueChange = { script = it }, label = { Text("脚本内容") },
                modifier = Modifier.fillMaxWidth().height(120.dp), maxLines = 6)
            OutlinedTextField(value = user, onValueChange = { user = it }, label = { Text("执行用户") },
                modifier = Modifier.fillMaxWidth(), singleLine = true)

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier.size(20.dp)
                        .background(if (raw) Color(0xFF2563EB) else Color(0xFFF3F4F6), RoundedCornerShape(4.dp))
                        .border(1.dp, if (raw) Color(0xFF2563EB) else Color(0xFFD1D5DB), RoundedCornerShape(4.dp))
                        .clickable { raw = !raw },
                    contentAlignment = Alignment.Center
                ) { if (raw) Text("✓", fontSize = 11.sp, color = Color.White) }
                Text("Raw 模式", fontSize = 13.sp, color = Color(0xFF71717A))
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier.size(20.dp)
                        .background(if (status) Color(0xFF059669) else Color(0xFFF3F4F6), RoundedCornerShape(4.dp))
                        .border(1.dp, if (status) Color(0xFF059669) else Color(0xFFD1D5DB), RoundedCornerShape(4.dp))
                        .clickable { status = !status },
                    contentAlignment = Alignment.Center
                ) { if (status) Text("✓", fontSize = 11.sp, color = Color.White) }
                Text("启用状态", fontSize = 13.sp, color = Color(0xFF71717A))
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("取消", color = Color(0xFF71717A)) }
                Spacer(Modifier.width(8.dp))
                val canSubmit = name.isNotBlank() && script.isNotBlank()
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(8.dp))
                        .background(if (canSubmit) Color(0xFF2563EB) else Color(0xFF71717A))
                        .clickable(enabled = canSubmit) { onSave(name.trim(), script, user.trim().ifEmpty { "root" }, raw, status) }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) { Text("保存", fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Medium) }
            }
        }
    }
}
