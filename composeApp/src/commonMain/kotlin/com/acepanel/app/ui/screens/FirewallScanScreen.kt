package com.acepanel.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
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
import com.acepanel.app.ui.components.StatusBarSpacer
import com.acepanel.app.ui.components.rememberTabBackStack
import com.acepanel.app.viewmodel.FirewallScanViewModel

@Composable
fun FirewallScanScreen(
    panelId: String,
    onBack: () -> Unit = {},
    showStatusBarSpacer: Boolean = true,
    showBackButton: Boolean = true
) {
    val vm: FirewallScanViewModel = viewModel()
    val setting by vm.setting.collectAsStateWithLifecycle()
    val summary by vm.summary.collectAsStateWithLifecycle()
    val topIps by vm.topIps.collectAsStateWithLifecycle()
    val topPorts by vm.topPorts.collectAsStateWithLifecycle()
    val events by vm.events.collectAsStateWithLifecycle()
    val eventTotal by vm.eventTotal.collectAsStateWithLifecycle()
    val isLoading by vm.isLoading.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    val actionError by vm.actionError.collectAsStateWithLifecycle()
    val startDate by vm.startDate.collectAsStateWithLifecycle()
    val endDate by vm.endDate.collectAsStateWithLifecycle()

    LaunchedEffect(panelId) { vm.init(panelId) }

    var showClearConfirm by remember { mutableStateOf(false) }
    val tabStack = rememberTabBackStack()
    val selectedTab = tabStack.current  // 0=汇总, 1=Top IP, 2=端口, 3=事件

    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
        if (showStatusBarSpacer) StatusBarSpacer()

        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showBackButton) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.5.dp, Color(0xFFE4E4E7), RoundedCornerShape(10.dp))
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) { Text("‹", fontSize = 20.sp, color = Color(0xFF18181B)) }
            } else {
                Spacer(modifier = Modifier.size(36.dp))
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("扫描审计", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF18181B))
                if (setting != null) {
                    val enabled = setting!!.enabled
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (enabled) Color(0xFFECFDF5) else Color(0xFFF4F4F5))
                            .clickable { vm.toggleEnabled() }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            if (enabled) "监控中" else "已停止",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (enabled) Color(0xFF10B981) else Color(0xFFA1A1AA)
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .height(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFFEF2F2))
                    .clickable { showClearConfirm = true }
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("清空", fontSize = 13.sp, color = Color(0xFFEF4444), fontWeight = FontWeight.Medium)
            }
        }

        // 日期范围
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = startDate,
                onValueChange = { vm.startDate.value = it },
                modifier = Modifier.weight(1f),
                label = { Text("开始", fontSize = 12.sp) },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF2563EB),
                    unfocusedBorderColor = Color(0xFFE4E4E7)
                )
            )
            OutlinedTextField(
                value = endDate,
                onValueChange = { vm.endDate.value = it },
                modifier = Modifier.weight(1f),
                label = { Text("结束", fontSize = 12.sp) },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF2563EB),
                    unfocusedBorderColor = Color(0xFFE4E4E7)
                )
            )
            Box(
                modifier = Modifier
                    .height(56.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF2563EB))
                    .clickable { vm.load() }
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                } else {
                    Text("查询", fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Medium)
                }
            }
        }

        // Tab 选择
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("汇总", "Top IP", "端口排行", "事件").forEachIndexed { index, label ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selectedTab == index) Color(0xFF2563EB) else Color(0xFFF4F4F5))
                        .clickable { tabStack.select(index) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        label,
                        fontSize = 13.sp,
                        fontWeight = if (selectedTab == index) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (selectedTab == index) Color.White else Color(0xFF71717A)
                    )
                }
            }
        }

        // 错误提示
        if (error != null || actionError != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFFEF2F2))
                    .padding(12.dp)
            ) {
                Text("⚠ ${actionError ?: error}", fontSize = 13.sp, color = Color(0xFFEF4444))
            }
        }

        LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
            when (selectedTab) {
                0 -> {
                    // 汇总
                    if (summary != null) {
                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    ScanStatCard("总扫描次数", summary!!.total_count.toString(), Modifier.weight(1f))
                                    ScanStatCard("唯一 IP", summary!!.unique_ips.toString(), Modifier.weight(1f))
                                    ScanStatCard("唯一端口", summary!!.unique_ports.toString(), Modifier.weight(1f))
                                }
                            }
                        }
                    }
                    // 扫描设置信息
                    if (setting != null) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(1.5.dp, Color(0xFFE4E4E7), RoundedCornerShape(12.dp))
                                    .padding(16.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        "扫描设置",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF71717A)
                                    )
                                    val s = setting!!
                                    Text("保留 ${s.days} 天", fontSize = 13.sp, color = Color(0xFF18181B))
                                    Text(
                                        "自动封禁: ${if (s.auto_block) "开启（阈值 ${s.block_threshold} 次/${s.block_window}分钟，封禁 ${if (s.block_duration == 0) "永久" else "${s.block_duration}h"}）" else "关闭"}",
                                        fontSize = 13.sp,
                                        color = Color(0xFF18181B)
                                    )
                                    if (s.interfaces.isNotEmpty()) {
                                        Text("监控网卡: ${s.interfaces.joinToString(", ")}", fontSize = 13.sp, color = Color(0xFF18181B))
                                    }
                                }
                            }
                        }
                    }
                    if (summary == null && !isLoading) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("暂无数据，请选择日期范围后查询", fontSize = 14.sp, color = Color(0xFFA1A1AA))
                            }
                        }
                    }
                }
                1 -> {
                    // Top IP
                    if (topIps.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(48.dp),
                                contentAlignment = Alignment.Center
                            ) { Text("暂无 IP 数据", fontSize = 14.sp, color = Color(0xFFA1A1AA)) }
                        }
                    } else {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(1.5.dp, Color(0xFFE4E4E7), RoundedCornerShape(12.dp))
                            ) {
                                Column {
                                    topIps.forEachIndexed { index, item ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(24.dp)
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(
                                                            when (index) {
                                                                0 -> Color(0xFFFEF3C7)
                                                                1 -> Color(0xFFF1F5F9)
                                                                2 -> Color(0xFFFFF7ED)
                                                                else -> Color(0xFFF4F4F5)
                                                            }
                                                        ),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        "${index + 1}",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF71717A)
                                                    )
                                                }
                                                Text(item.key, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF18181B))
                                            }
                                            Text(
                                                "${item.count} 次",
                                                fontSize = 13.sp,
                                                color = Color(0xFFEF4444),
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                        if (index < topIps.lastIndex) HorizontalDivider(thickness = 1.dp, color = Color(0xFFE4E4E7))
                                    }
                                }
                            }
                        }
                    }
                }
                2 -> {
                    // 端口排行
                    if (topPorts.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(48.dp),
                                contentAlignment = Alignment.Center
                            ) { Text("暂无端口数据", fontSize = 14.sp, color = Color(0xFFA1A1AA)) }
                        }
                    } else {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(1.5.dp, Color(0xFFE4E4E7), RoundedCornerShape(12.dp))
                            ) {
                                Column {
                                    topPorts.forEachIndexed { index, item ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(Color(0xFFEFF6FF))
                                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        "#${index + 1}",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF2563EB)
                                                    )
                                                }
                                                Text(
                                                    "端口 ${item.key}",
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = Color(0xFF18181B)
                                                )
                                            }
                                            Text(
                                                "${item.count} 次",
                                                fontSize = 13.sp,
                                                color = Color(0xFFEF4444),
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                        if (index < topPorts.lastIndex) HorizontalDivider(thickness = 1.dp, color = Color(0xFFE4E4E7))
                                    }
                                }
                            }
                        }
                    }
                }
                3 -> {
                    // 事件列表
                    if (events.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(48.dp),
                                contentAlignment = Alignment.Center
                            ) { Text("暂无扫描事件", fontSize = 14.sp, color = Color(0xFFA1A1AA)) }
                        }
                    } else {
                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    "共 $eventTotal 条记录",
                                    fontSize = 12.sp,
                                    color = Color(0xFF71717A)
                                )
                                events.forEach { event ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .border(1.5.dp, Color(0xFFE4E4E7), RoundedCornerShape(10.dp))
                                            .padding(12.dp)
                                    ) {
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    event.source_ip,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Color(0xFF18181B)
                                                )
                                                Text(
                                                    "${event.count} 次",
                                                    fontSize = 13.sp,
                                                    color = Color(0xFFEF4444),
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                ScanEventTag("端口 ${event.port}", Color(0xFF2563EB), Color(0xFFEFF6FF))
                                                ScanEventTag(event.protocol.uppercase(), Color(0xFF7C3AED), Color(0xFFF5F3FF))
                                                if (event.country.isNotEmpty()) {
                                                    ScanEventTag(event.country, Color(0xFF059669), Color(0xFFECFDF5))
                                                }
                                            }
                                            if (event.last_seen.isNotEmpty()) {
                                                Text(
                                                    "最后: ${event.last_seen.take(19).replace("T", " ")}",
                                                    fontSize = 11.sp,
                                                    color = Color(0xFFA1A1AA)
                                                )
                                            }
                                        }
                                    }
                                }
                                // 加载更多
                                if (events.size < eventTotal) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .border(1.5.dp, Color(0xFFE4E4E7), RoundedCornerShape(10.dp))
                                            .clickable { vm.loadMoreEvents() }
                                            .padding(vertical = 14.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("加载更多", fontSize = 14.sp, color = Color(0xFF2563EB))
                                    }
                                }
                            }
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }

    // 清空确认
    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("清空扫描数据", fontWeight = FontWeight.SemiBold) },
            text = { Text("此操作将永久删除所有扫描记录，不可恢复。确认继续？") },
            confirmButton = {
                TextButton(onClick = { vm.clearData(); showClearConfirm = false }) {
                    Text("确认清空", color = Color(0xFFEF4444))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text("取消", color = Color(0xFF2563EB))
                }
            }
        )
    }
}

@Composable
private fun ScanStatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .border(1.5.dp, Color(0xFFE4E4E7), RoundedCornerShape(12.dp))
            .padding(14.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF18181B))
            Text(label, fontSize = 11.sp, color = Color(0xFF71717A))
        }
    }
}

@Composable
private fun ScanEventTag(text: String, textColor: Color, bgColor: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bgColor)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(text, fontSize = 11.sp, color = textColor, fontWeight = FontWeight.Medium)
    }
}
