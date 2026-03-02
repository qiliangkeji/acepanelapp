package org.example.project.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
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
import org.example.project.data.TaskItem
import org.example.project.ui.components.StatusBarSpacer
import org.example.project.viewmodel.TaskCenterViewModel

@Composable
fun TaskCenterScreen(
    panelId: String = "",
    onBack: () -> Unit
) {
    val vm = viewModel<TaskCenterViewModel>()
    val tasks by vm.tasks.collectAsStateWithLifecycle()
    val hasRunning by vm.hasRunning.collectAsStateWithLifecycle()
    val isLoading by vm.isLoading.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()

    LaunchedEffect(panelId) {
        if (panelId.isNotEmpty()) vm.init(panelId)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        StatusBarSpacer()
        // Header
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
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "任务中心",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF18181B)
                )
                if (hasRunning) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(Color(0xFF22C55E), RoundedCornerShape(4.dp))
                    )
                }
            }
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .border(1.5.dp, Color(0xFFE4E4E7), RoundedCornerShape(10.dp))
                    .clickable { vm.load() },
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color(0xFF2563EB))
                } else {
                    Text(text = "↻", fontSize = 16.sp, color = Color(0xFF18181B))
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

        if (tasks.isEmpty() && !isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("暂无任务", fontSize = 14.sp, color = Color(0xFFA1A1AA))
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(tasks) { task ->
                    TaskCard(task = task, onDelete = { vm.deleteTask(task.id) })
                }
            }
        }
    }
}

@Composable
private fun TaskCard(task: TaskItem, onDelete: () -> Unit) {
    val (statusColor, statusBg, statusLabel) = when (task.status) {
        "running" -> Triple(Color(0xFF2563EB), Color(0xFFEFF6FF), "运行中")
        "waiting" -> Triple(Color(0xFFD97706), Color(0xFFFEF3C7), "等待中")
        "finished" -> Triple(Color(0xFF059669), Color(0xFFECFDF5), "已完成")
        "failed" -> Triple(Color(0xFFEF4444), Color(0xFFFEF2F2), "失败")
        else -> Triple(Color(0xFF71717A), Color(0xFFF3F4F6), task.status)
    }
    val canDelete = task.status == "finished" || task.status == "failed"

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
            Text(
                text = task.name,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF18181B),
                modifier = Modifier.weight(1f)
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .height(22.dp)
                        .background(statusBg, RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(statusLabel, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = statusColor)
                }
                if (canDelete) {
                    Box(
                        modifier = Modifier
                            .height(22.dp)
                            .background(Color(0xFFFEF2F2), RoundedCornerShape(4.dp))
                            .clickable(onClick = onDelete)
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("删除", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFEF4444))
                    }
                }
            }
        }
        val timeText = buildString {
            val t = task.updated_at.ifEmpty { task.created_at }
            if (t.isNotEmpty()) append(t.take(19).replace("T", " "))
        }
        if (timeText.isNotEmpty()) {
            Text(text = timeText, fontSize = 12.sp, color = Color(0xFFA1A1AA))
        }
        if (task.log.isNotEmpty()) {
            val preview = task.log.lines().takeLast(2).joinToString("\n")
            Text(
                text = preview,
                fontSize = 11.sp,
                color = Color(0xFF71717A),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF9FAFB), RoundedCornerShape(6.dp))
                    .padding(8.dp)
            )
        }
    }
}
