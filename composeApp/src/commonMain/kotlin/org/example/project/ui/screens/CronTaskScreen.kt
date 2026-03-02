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
import org.example.project.data.CronTaskApiItem
import org.example.project.ui.components.StatusBarSpacer
import org.example.project.viewmodel.CronTaskViewModel

@Composable
fun CronTaskScreen(
    panelId: String = "",
    onBack: () -> Unit,
    onAddClick: () -> Unit = {}
) {
    val vm = viewModel<CronTaskViewModel>()
    val tasks by vm.tasks.collectAsStateWithLifecycle()
    val total by vm.total.collectAsStateWithLifecycle()
    val isLoading by vm.isLoading.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    val actionError by vm.actionError.collectAsStateWithLifecycle()
    val showCreateDialog by vm.showCreateDialog.collectAsStateWithLifecycle()
    val editingTask by vm.editingTask.collectAsStateWithLifecycle()

    LaunchedEffect(panelId) {
        if (panelId.isNotEmpty()) vm.init(panelId)
    }

    if (showCreateDialog) {
        CreateCronDialog(
            actionError = actionError,
            onDismiss = { vm.hideCreate() },
            onCreate = { name, type, cronTime, keep, script, url, subType, targets ->
                vm.createTask(name, type, cronTime, keep, script, url, subType, targets)
            }
        )
    }

    editingTask?.let { task ->
        EditCronDialog(
            task = task,
            actionError = actionError,
            onDismiss = { vm.hideEdit() },
            onUpdate = { name, cronTime, keep, script, url, subType, targets ->
                vm.updateTask(task.id, name, cronTime, keep, script, url, subType, targets)
            }
        )
    }

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
                Text("计划任务", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF18181B))
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color(0xFF2563EB))
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier.size(36.dp).border(1.5.dp, Color(0xFFE4E4E7), RoundedCornerShape(10.dp))
                        .clickable { vm.load() },
                    contentAlignment = Alignment.Center
                ) { Text("↻", fontSize = 16.sp, color = Color(0xFF18181B)) }
                Box(
                    modifier = Modifier.size(36.dp).background(Color(0xFF2563EB), RoundedCornerShape(10.dp))
                        .clickable { vm.showCreate() },
                    contentAlignment = Alignment.Center
                ) { Text("+", fontSize = 20.sp, color = Color.White, fontWeight = FontWeight.Bold) }
            }
        }

        if (error != null) Text("加载失败: $error", fontSize = 13.sp, color = Color(0xFFEF4444),
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))

        if (tasks.isEmpty() && !isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(if (panelId.isEmpty()) "请先选择面板" else "暂无计划任务", fontSize = 14.sp, color = Color(0xFFA1A1AA))
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(tasks) { task ->
                    CronTaskCard(task = task, onToggle = { vm.toggleStatus(task) }, onDelete = { vm.deleteTask(task.id) }, onEdit = { vm.showEdit(task) })
                }
            }
        }
    }
}

@Composable
private fun CreateCronDialog(
    actionError: String?,
    onDismiss: () -> Unit,
    onCreate: (String, String, String, Int, String, String, String, List<String>) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("shell") }
    var cronTime by remember { mutableStateOf("0 * * * *") }
    var keep by remember { mutableStateOf("7") }
    var script by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }

    val typeOptions = listOf("shell" to "Shell 脚本", "backup" to "备份", "url" to "URL 请求", "synctime" to "同步时间")

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("新建计划任务", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF18181B))

            OutlinedTextField(value = name, onValueChange = { name = it },
                label = { Text("任务名称") }, modifier = Modifier.fillMaxWidth(), singleLine = true)

            // Type selector
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("任务类型", fontSize = 13.sp, color = Color(0xFF71717A))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    typeOptions.forEach { (key, label) ->
                        Box(
                            modifier = Modifier
                                .height(30.dp)
                                .background(
                                    if (type == key) Color(0xFF2563EB) else Color(0xFFF3F4F6),
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { type = key }
                                .padding(horizontal = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(label, fontSize = 12.sp, color = if (type == key) Color.White else Color(0xFF71717A))
                        }
                    }
                }
            }

            OutlinedTextField(value = cronTime, onValueChange = { cronTime = it },
                label = { Text("Cron 表达式") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Text("示例: 0 * * * *（每小时）、0 2 * * *（每天2时）", fontSize = 11.sp, color = Color(0xFFA1A1AA))

            OutlinedTextField(value = keep, onValueChange = { keep = it },
                label = { Text("保留份数") }, modifier = Modifier.fillMaxWidth(), singleLine = true)

            if (type == "shell") {
                OutlinedTextField(value = script, onValueChange = { script = it },
                    label = { Text("脚本内容") }, modifier = Modifier.fillMaxWidth().height(80.dp), maxLines = 4)
            } else if (type == "url") {
                OutlinedTextField(value = url, onValueChange = { url = it },
                    label = { Text("请求 URL") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            }

            if (actionError != null) {
                Text(actionError, fontSize = 12.sp, color = Color(0xFFEF4444))
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("取消", color = Color(0xFF71717A)) }
                Spacer(Modifier.width(8.dp))
                val keepInt = keep.toIntOrNull() ?: 7
                val canSubmit = name.isNotBlank() && cronTime.isNotBlank()
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (canSubmit) Color(0xFF2563EB) else Color(0xFF71717A))
                        .clickable(enabled = canSubmit) {
                            onCreate(name.trim(), type, cronTime.trim(), keepInt,
                                script, url, "", emptyList())
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
private fun EditCronDialog(
    task: CronTaskApiItem,
    actionError: String?,
    onDismiss: () -> Unit,
    onUpdate: (String, String, Int, String, String, String, List<String>) -> Unit
) {
    var name by remember { mutableStateOf(task.name) }
    var cronTime by remember { mutableStateOf(task.time) }
    var keep by remember { mutableStateOf("7") }
    var script by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    val type = task.type

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("编辑计划任务", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF18181B))

            OutlinedTextField(value = name, onValueChange = { name = it },
                label = { Text("任务名称") }, modifier = Modifier.fillMaxWidth(), singleLine = true)

            OutlinedTextField(value = cronTime, onValueChange = { cronTime = it },
                label = { Text("Cron 表达式") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Text("示例: 0 * * * *（每小时）、0 2 * * *（每天2时）", fontSize = 11.sp, color = Color(0xFFA1A1AA))

            OutlinedTextField(value = keep, onValueChange = { keep = it },
                label = { Text("保留份数") }, modifier = Modifier.fillMaxWidth(), singleLine = true)

            if (type == "shell") {
                OutlinedTextField(value = script, onValueChange = { script = it },
                    label = { Text("脚本内容") }, modifier = Modifier.fillMaxWidth().height(80.dp), maxLines = 4)
            } else if (type == "url") {
                OutlinedTextField(value = url, onValueChange = { url = it },
                    label = { Text("请求 URL") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            }

            if (actionError != null) {
                Text(actionError, fontSize = 12.sp, color = Color(0xFFEF4444))
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("取消", color = Color(0xFF71717A)) }
                Spacer(Modifier.width(8.dp))
                val keepInt = keep.toIntOrNull() ?: 7
                val canSubmit = name.isNotBlank() && cronTime.isNotBlank()
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (canSubmit) Color(0xFF2563EB) else Color(0xFF71717A))
                        .clickable(enabled = canSubmit) {
                            onUpdate(name.trim(), cronTime.trim(), keepInt, script, url, "", emptyList())
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text("保存", fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
fun CronTaskCard(task: CronTaskApiItem, onToggle: () -> Unit = {}, onDelete: () -> Unit = {}, onEdit: () -> Unit = {}) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, Color(0xFFE4E4E7), RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(task.name, fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                color = if (task.status) Color(0xFF18181B) else Color(0xFFA1A1AA),
                modifier = Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier.width(50.dp).height(28.dp)
                        .background(if (task.status) Color(0xFF2563EB) else Color(0xFFE4E4E7), RoundedCornerShape(14.dp))
                        .clickable(onClick = onToggle).padding(2.dp),
                    contentAlignment = if (task.status) Alignment.CenterEnd else Alignment.CenterStart
                ) { Box(modifier = Modifier.size(24.dp).background(Color.White, RoundedCornerShape(12.dp))) }
                Box(
                    modifier = Modifier.size(28.dp).background(Color(0xFFF3F4F6), RoundedCornerShape(8.dp))
                        .clickable(onClick = onEdit),
                    contentAlignment = Alignment.Center
                ) { Text("✎", fontSize = 13.sp, color = Color(0xFF2563EB)) }
                Box(
                    modifier = Modifier.size(28.dp).background(Color(0xFFF3F4F6), RoundedCornerShape(8.dp))
                        .clickable(onClick = onDelete),
                    contentAlignment = Alignment.Center
                ) { Text("×", fontSize = 16.sp, color = Color(0xFF71717A)) }
            }
        }
        Box(
            modifier = Modifier.height(26.dp).background(Color(0xFFF3F4F6), RoundedCornerShape(6.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) { Text(task.time, fontSize = 12.sp, color = if (task.status) Color(0xFF71717A) else Color(0xFFA1A1AA)) }
        Text("类型: ${task.type}", fontSize = 13.sp, color = Color(0xFFA1A1AA))
    }
}
