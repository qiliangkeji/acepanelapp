package com.acepanel.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.acepanel.app.data.LogApiEntry
import com.acepanel.app.data.SshLogApiItem
import com.acepanel.app.ui.components.StatusBarSpacer
import com.acepanel.app.viewmodel.LogViewViewModel

@Composable
fun LogViewScreen(
    panelId: String = "",
    onBack: () -> Unit
) {
    val vm = viewModel<LogViewViewModel>()
    val selectedTypeIndex by vm.selectedTypeIndex.collectAsStateWithLifecycle()
    val logs by vm.logs.collectAsStateWithLifecycle()
    val sshLogs by vm.sshLogs.collectAsStateWithLifecycle()
    val dates by vm.dates.collectAsStateWithLifecycle()
    val selectedDate by vm.selectedDate.collectAsStateWithLifecycle()
    val isLoading by vm.isLoading.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()

    val logTypes = listOf("面板日志", "数据库日志", "访问日志", "SSH 审计")

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
                .padding(horizontal = 24.dp, vertical = 12.dp),
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
                    text = "日志查看",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF18181B)
                )
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = Color(0xFF2563EB)
                    )
                }
            }

            Box(modifier = Modifier.size(36.dp))
        }

        // Log Types Tab
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(logTypes.size) { index ->
                val isSelected = index == selectedTypeIndex
                Box(
                    modifier = Modifier
                        .height(32.dp)
                        .background(
                            if (isSelected) Color(0xFF2563EB) else Color(0xFFF3F4F6),
                            RoundedCornerShape(16.dp)
                        )
                        .clickable { vm.selectType(index) }
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = logTypes[index],
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (isSelected) Color.White else Color(0xFF71717A)
                    )
                }
            }
        }

        // Date Picker (only for app/db/http logs, not SSH)
        if (selectedTypeIndex < 3 && dates.isNotEmpty()) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item {
                    val isSelected = selectedDate.isEmpty()
                    Box(
                        modifier = Modifier
                            .height(26.dp)
                            .background(
                                if (isSelected) Color(0xFF18181B) else Color(0xFFF3F4F6),
                                RoundedCornerShape(13.dp)
                            )
                            .clickable { vm.selectDate("") }
                            .padding(horizontal = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "今天",
                            fontSize = 11.sp,
                            color = if (isSelected) Color.White else Color(0xFF71717A)
                        )
                    }
                }
                items(dates) { date ->
                    val isSelected = date == selectedDate
                    Box(
                        modifier = Modifier
                            .height(26.dp)
                            .background(
                                if (isSelected) Color(0xFF18181B) else Color(0xFFF3F4F6),
                                RoundedCornerShape(13.dp)
                            )
                            .clickable { vm.selectDate(date) }
                            .padding(horizontal = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = date,
                            fontSize = 11.sp,
                            color = if (isSelected) Color.White else Color(0xFF71717A)
                        )
                    }
                }
            }
        }

        if (error != null) {
            Text(
                text = "加载失败: $error",
                fontSize = 13.sp,
                color = Color(0xFFEF4444),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
            )
        }

        // Log List
        if (selectedTypeIndex == 3) {
            // SSH logs
            if (sshLogs.isEmpty() && !isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (panelId.isEmpty()) "请先选择面板" else "暂无 SSH 记录",
                        fontSize = 14.sp, color = Color(0xFFA1A1AA)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 24.dp, end = 24.dp, top = 4.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(sshLogs) { item -> SshLogCard(item) }
                }
            }
        } else {
            // App / DB logs
            if (logs.isEmpty() && !isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (panelId.isEmpty()) "请先选择面板" else "暂无日志",
                        fontSize = 14.sp, color = Color(0xFFA1A1AA)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 24.dp, end = 24.dp, top = 4.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(logs) { entry -> LogEntryCard(entry) }
                }
            }
        }
    }
}

@Composable
private fun LogEntryCard(entry: LogApiEntry) {
    val levelColor = when (entry.level) {
        "ERROR", "error" -> Color(0xFFEF4444)
        "WARN", "warn", "WARNING", "warning" -> Color(0xFFF59E0B)
        else -> Color(0xFF3B82F6)
    }
    val borderColor = if (entry.level in listOf("ERROR", "error"))
        Color(0xFFFECACA) else Color(0xFFE4E4E7)
    val contentColor = if (entry.level in listOf("ERROR", "error"))
        Color(0xFFDC2626) else Color(0xFF18181B)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, borderColor, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(levelColor, RoundedCornerShape(4.dp))
            )
            Text(
                text = entry.time.take(19).replace("T", " "),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF71717A)
            )
            if (entry.operator_name.isNotEmpty()) {
                Text(
                    text = entry.operator_name,
                    fontSize = 11.sp,
                    color = Color(0xFFA1A1AA)
                )
            }
        }
        Text(text = entry.msg, fontSize = 13.sp, color = contentColor)
    }
}

@Composable
private fun SshLogCard(item: SshLogApiItem) {
    val (levelColor, bgColor) = when (item.status) {
        "accepted" -> Pair(Color(0xFF10B981), Color(0xFFECFDF5))
        "failed", "invalid_user" -> Pair(Color(0xFFEF4444), Color(0xFFFEE2E2))
        else -> Pair(Color(0xFF6B7280), Color(0xFFF3F4F6))
    }
    val statusText = when (item.status) {
        "accepted" -> "成功"
        "failed" -> "失败"
        "invalid_user" -> "无效用户"
        "disconnected" -> "断开"
        else -> item.status
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, Color(0xFFE4E4E7), RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
            Text(
                text = "${item.user}@${item.ip}:${item.port}",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF18181B)
            )
            Text(
                text = "${item.time.take(19).replace("T", " ")} · ${item.method}",
                fontSize = 12.sp,
                color = Color(0xFF71717A)
            )
        }
        Box(
            modifier = Modifier
                .height(24.dp)
                .background(bgColor, RoundedCornerShape(6.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = statusText,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = levelColor
            )
        }
    }
}
