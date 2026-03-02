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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.example.project.data.ProjectListItem
import org.example.project.ui.components.StatusBarSpacer
import org.example.project.viewmodel.ProjectListViewModel

@Composable
fun ProjectListScreen(
    panelId: String = "",
    onBack: () -> Unit
) {
    val vm = viewModel<ProjectListViewModel>()
    val projects by vm.projects.collectAsStateWithLifecycle()
    val typeFilter by vm.typeFilter.collectAsStateWithLifecycle()
    val isLoading by vm.isLoading.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    val actingName by vm.actingName.collectAsStateWithLifecycle()
    val actionError by vm.actionError.collectAsStateWithLifecycle()
    val showCreateDialog by vm.showCreateDialog.collectAsStateWithLifecycle()

    LaunchedEffect(panelId) {
        if (panelId.isNotEmpty()) vm.init(panelId)
    }

    if (showCreateDialog) {
        CreateProjectDialog(
            actionError = actionError,
            onDismiss = { vm.hideCreate() },
            onCreate = { name, type, desc, rootDir, execStart, user ->
                vm.createProject(name, type, desc, rootDir, execStart, user)
            }
        )
    }

    val typeItems = listOf(
        "all" to "全部",
        "general" to "通用",
        "php" to "PHP",
        "java" to "Java",
        "go" to "Go",
        "python" to "Python",
        "nodejs" to "Node.js",
        "dotnet" to ".NET"
    )

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
                .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "项目管理",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF18181B)
                    )
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color(0xFF2563EB))
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
        }

        // 类型过滤
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(typeItems) { (key, label) ->
                val isSelected = typeFilter == key
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

        if (error != null) {
            Text(
                text = "加载失败: $error",
                fontSize = 12.sp,
                color = Color(0xFFEF4444),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
            )
        }
        if (actionError != null) {
            Text(
                text = actionError!!,
                fontSize = 12.sp,
                color = Color(0xFFEF4444),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
            )
        }

        if (projects.isEmpty() && !isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = if (panelId.isEmpty()) "请先选择面板" else "暂无项目",
                    fontSize = 14.sp, color = Color(0xFFA1A1AA)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(projects) { project ->
                    ProjectCard(
                        project = project,
                        isActing = actingName == project.name,
                        onStart = { vm.start(project.name) },
                        onStop = { vm.stop(project.name) },
                        onRestart = { vm.restart(project.name) },
                        onToggleEnable = { vm.toggleEnable(project) },
                        onDelete = { vm.delete(project.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ProjectCard(
    project: ProjectListItem,
    isActing: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onRestart: () -> Unit,
    onToggleEnable: () -> Unit,
    onDelete: () -> Unit = {}
) {
    val isActive = project.status.startsWith("active")
    val (statusColor, statusBg, statusLabel) = when {
        isActive -> Triple(Color(0xFF059669), Color(0xFFECFDF5), "运行中")
        project.status.startsWith("failed") -> Triple(Color(0xFFEF4444), Color(0xFFFEF2F2), "失败")
        project.status.isEmpty() -> Triple(Color(0xFFA1A1AA), Color(0xFFF3F4F6), "未知")
        else -> Triple(Color(0xFF71717A), Color(0xFFF3F4F6), "停止")
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, Color(0xFFE4E4E7), RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 顶部：名称 + 类型 + 状态
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = project.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF18181B)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFF3F4F6), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(project.type.uppercase(), fontSize = 10.sp, color = Color(0xFF71717A))
                    }
                    if (project.uptime.isNotEmpty()) {
                        Text(project.uptime, fontSize = 12.sp, color = Color(0xFFA1A1AA))
                    }
                }
            }
            if (isActing) {
                CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = Color(0xFF2563EB))
            } else {
                Box(
                    modifier = Modifier
                        .height(24.dp)
                        .background(statusBg, RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(statusLabel, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = statusColor)
                }
            }
        }

        // 目录
        if (project.root_dir.isNotEmpty()) {
            Text(project.root_dir, fontSize = 12.sp, color = Color(0xFF71717A))
        }

        // 操作按钮行
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ProjActionBtn("启动", Color(0xFF2563EB), Color.White, Modifier.weight(1f), !isActing, onStart)
            ProjActionBtn("停止", Color.White, Color(0xFFEF4444), Modifier.weight(1f), !isActing, onStop,
                borderColor = Color(0xFFEF4444))
            ProjActionBtn("重启", Color.White, Color(0xFF18181B), Modifier.weight(1f), !isActing, onRestart,
                borderColor = Color(0xFFE4E4E7))
            // 自启动 toggle
            Box(
                modifier = Modifier
                    .height(32.dp)
                    .background(
                        if (project.enabled) Color(0xFFECFDF5) else Color(0xFFF3F4F6),
                        RoundedCornerShape(8.dp)
                    )
                    .clickable(enabled = !isActing, onClick = onToggleEnable)
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (project.enabled) "自启动" else "未自启",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (project.enabled) Color(0xFF059669) else Color(0xFF71717A)
                )
            }
            Box(
                modifier = Modifier.size(32.dp)
                    .background(Color(0xFFFEF2F2), RoundedCornerShape(8.dp))
                    .clickable(enabled = !isActing, onClick = onDelete),
                contentAlignment = Alignment.Center
            ) {
                Text("×", fontSize = 16.sp, color = Color(0xFFEF4444))
            }
        }
    }
}

@Composable
private fun ProjActionBtn(
    text: String,
    bgColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
    borderColor: Color = Color.Transparent
) {
    Box(
        modifier = modifier
            .height(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .then(if (borderColor != Color.Transparent) Modifier.border(1.dp, borderColor, RoundedCornerShape(8.dp)) else Modifier)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = textColor)
    }
}

@Composable
private fun CreateProjectDialog(
    actionError: String?,
    onDismiss: () -> Unit,
    onCreate: (String, String, String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("general") }
    var description by remember { mutableStateOf("") }
    var rootDir by remember { mutableStateOf("") }
    var execStart by remember { mutableStateOf("") }
    var user by remember { mutableStateOf("root") }

    val typeOptions = listOf("general" to "通用", "php" to "PHP", "java" to "Java",
        "go" to "Go", "python" to "Python", "nodejs" to "Node.js")

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                .background(Color.White).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("新建项目", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF18181B))

            OutlinedTextField(value = name, onValueChange = { name = it },
                label = { Text("项目名称") }, modifier = Modifier.fillMaxWidth(), singleLine = true)

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("项目类型", fontSize = 13.sp, color = Color(0xFF71717A))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    typeOptions.take(3).forEach { (key, label) ->
                        Box(
                            modifier = Modifier.height(28.dp)
                                .background(if (type == key) Color(0xFF2563EB) else Color(0xFFF3F4F6), RoundedCornerShape(6.dp))
                                .clickable { type = key }.padding(horizontal = 10.dp),
                            contentAlignment = Alignment.Center
                        ) { Text(label, fontSize = 12.sp, color = if (type == key) Color.White else Color(0xFF71717A)) }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    typeOptions.drop(3).forEach { (key, label) ->
                        Box(
                            modifier = Modifier.height(28.dp)
                                .background(if (type == key) Color(0xFF2563EB) else Color(0xFFF3F4F6), RoundedCornerShape(6.dp))
                                .clickable { type = key }.padding(horizontal = 10.dp),
                            contentAlignment = Alignment.Center
                        ) { Text(label, fontSize = 12.sp, color = if (type == key) Color.White else Color(0xFF71717A)) }
                    }
                }
            }

            OutlinedTextField(value = rootDir, onValueChange = { rootDir = it },
                label = { Text("项目目录") }, placeholder = { Text("/opt/myapp") },
                modifier = Modifier.fillMaxWidth(), singleLine = true)

            OutlinedTextField(value = execStart, onValueChange = { execStart = it },
                label = { Text("启动命令") }, placeholder = { Text("/usr/bin/java -jar app.jar") },
                modifier = Modifier.fillMaxWidth(), singleLine = true)

            OutlinedTextField(value = user, onValueChange = { user = it },
                label = { Text("运行用户") }, modifier = Modifier.fillMaxWidth(), singleLine = true)

            if (actionError != null) {
                Text(actionError, fontSize = 12.sp, color = Color(0xFFEF4444))
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("取消", color = Color(0xFF71717A)) }
                Spacer(Modifier.width(8.dp))
                val canSubmit = name.isNotBlank()
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(8.dp))
                        .background(if (canSubmit) Color(0xFF2563EB) else Color(0xFF71717A))
                        .clickable(enabled = canSubmit) {
                            onCreate(name.trim(), type, description, rootDir.trim(), execStart.trim(), user.trim())
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) { Text("创建", fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Medium) }
            }
        }
    }
}
