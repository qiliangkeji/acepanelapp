package com.acepanel.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.acepanel.app.data.MonitorHistoryResponse
import com.acepanel.app.data.MonitorSetting
import com.acepanel.app.data.ProcessApiItem
import com.acepanel.app.ui.components.AnimatedAppDialog
import com.acepanel.app.ui.components.StatusBarSpacer
import com.acepanel.app.ui.components.rememberTabBackStack
import com.acepanel.app.viewmodel.SystemMonitorViewModel

@Composable
fun SystemMonitorScreen(
    panelId: String = "",
    onBack: () -> Unit
) {
    val vm = viewModel<SystemMonitorViewModel>()
    val currentUsage by vm.currentUsage.collectAsStateWithLifecycle()
    val topProcesses by vm.topProcesses.collectAsStateWithLifecycle()
    val processType by vm.processType.collectAsStateWithLifecycle()
    val isLoading by vm.isLoading.collectAsStateWithLifecycle()
    val netSentSpeed by vm.netSentSpeed.collectAsStateWithLifecycle()
    val netRecvSpeed by vm.netRecvSpeed.collectAsStateWithLifecycle()
    val processList by vm.processList.collectAsStateWithLifecycle()
    val processTotal by vm.processTotal.collectAsStateWithLifecycle()
    val isProcessLoading by vm.isProcessLoading.collectAsStateWithLifecycle()
    val processSort by vm.processSort.collectAsStateWithLifecycle()
    val monitorHistory by vm.monitorHistory.collectAsStateWithLifecycle()
    val isHistoryLoading by vm.isHistoryLoading.collectAsStateWithLifecycle()
    val showSettingDialog by vm.showSettingDialog.collectAsStateWithLifecycle()
    val monitorSetting by vm.monitorSetting.collectAsStateWithLifecycle()

    val tabStack = rememberTabBackStack()
    val selectedTab = tabStack.current // 0=实时监控 1=进程管理 2=历史趋势

    LaunchedEffect(panelId) {
        if (panelId.isNotEmpty()) vm.init(panelId)
    }

    if (showSettingDialog) {
        MonitorSettingDialog(
            setting = monitorSetting,
            onDismiss = { vm.hideSetting() },
            onSave = { enabled, days, interval -> vm.saveSetting(enabled, days, interval) },
            onClear = { vm.clearHistory() }
        )
    }

    val cpuPercent = currentUsage?.percent ?: 0.0
    val memPercent = currentUsage?.mem?.usedPercent ?: 0.0
    val load1 = currentUsage?.load?.load1 ?: 0.0
    val load5 = currentUsage?.load?.load5 ?: 0.0
    val load15 = currentUsage?.load?.load15 ?: 0.0

    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
        StatusBarSpacer()
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(36.dp).border(1.5.dp, Color(0xFFE4E4E7), RoundedCornerShape(10.dp))
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) { Text("←", fontSize = 18.sp, color = Color(0xFF18181B)) }

            Text("系统监控", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF18181B))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (selectedTab == 2) {
                    Box(
                        modifier = Modifier.size(36.dp).border(1.5.dp, Color(0xFFE4E4E7), RoundedCornerShape(10.dp))
                            .clickable { vm.showSetting() },
                        contentAlignment = Alignment.Center
                    ) { Text("⚙", fontSize = 16.sp, color = Color(0xFF18181B)) }
                }
                Box(
                    modifier = Modifier.size(36.dp).border(1.5.dp, Color(0xFFE4E4E7), RoundedCornerShape(10.dp))
                        .clickable {
                            when (selectedTab) {
                                0 -> vm.refresh()
                                1 -> vm.loadProcessList()
                                2 -> vm.loadMonitorHistory()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) { Text("↻", fontSize = 16.sp, color = Color(0xFF18181B)) }
            }
        }

        // Tab bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("实时监控", "进程管理", "历史趋势").forEachIndexed { index, label ->
                Box(
                    modifier = Modifier.height(34.dp)
                        .background(
                            if (selectedTab == index) Color(0xFF18181B) else Color(0xFFF3F4F6),
                            RoundedCornerShape(10.dp)
                        )
                        .clickable {
                            tabStack.select(index)
                            when (index) {
                                1 -> if (processList.isEmpty()) vm.loadProcessList()
                                2 -> if (monitorHistory == null) vm.loadMonitorHistory()
                            }
                        }
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium,
                        color = if (selectedTab == index) Color.White else Color(0xFF71717A))
                }
            }
        }

        when (selectedTab) {
            0 -> MonitorTab(
                cpuPercent = cpuPercent, memPercent = memPercent,
                load1 = load1, load5 = load5, load15 = load15,
                netSentSpeed = netSentSpeed, netRecvSpeed = netRecvSpeed,
                topProcesses = topProcesses, processType = processType,
                panelId = panelId, onTypeChange = { vm.loadTopProcesses(it) }
            )
            1 -> ProcessTab(
                processes = processList,
                total = processTotal,
                isLoading = isProcessLoading,
                sort = processSort,
                onSortChange = { vm.loadProcessList(it) },
                onKill = { vm.killProcess(it) },
                onSignal = { pid, sig -> vm.sendSignal(pid, sig) }
            )
            2 -> HistoryTab(
                history = monitorHistory,
                isLoading = isHistoryLoading
            )
        }
    }
}

@Composable
private fun MonitorTab(
    cpuPercent: Double, memPercent: Double,
    load1: Double, load5: Double, load15: Double,
    netSentSpeed: Long, netRecvSpeed: Long,
    topProcesses: List<com.acepanel.app.data.ProcessStatItem>,
    processType: String,
    panelId: String,
    onTypeChange: (String) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Column(
                modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 4.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MonitorStatCard("CPU", "${cpuPercent.fmt1()}%", (cpuPercent / 100).toFloat().coerceIn(0f, 1f),
                    Color(0xFFEFF6FF), Color(0xFF2563EB), Color(0xFFBFDBFE), Color(0xFF2563EB))
                MonitorStatCard("内存", "${memPercent.fmt1()}%", (memPercent / 100).toFloat().coerceIn(0f, 1f),
                    Color(0xFFECFDF5), Color(0xFF059669), Color(0xFFA7F3D0), Color(0xFF059669))
                Column(
                    modifier = Modifier.fillMaxWidth().background(Color(0xFFFEF3C7), RoundedCornerShape(16.dp)).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("负载", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFD97706))
                    Text(load1.fmt2(), fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD97706))
                    Text("1min: ${load1.fmt2()} · 5min: ${load5.fmt2()} · 15min: ${load15.fmt2()}",
                        fontSize = 11.sp, color = Color(0xFFD97706))
                }
            }
        }
        item {
            Column(
                modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 4.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("网络流量", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF71717A), letterSpacing = 1.sp)
                Column(
                    modifier = Modifier.fillMaxWidth().border(1.5.dp, Color(0xFFE4E4E7), RoundedCornerShape(16.dp)).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    NetSpeedRow("↑", "上传", netSentSpeed, Color(0xFF059669))
                    NetSpeedRow("↓", "下载", netRecvSpeed, Color(0xFF3B82F6))
                }
            }
        }
        item {
            Column(
                modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 4.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("cpu" to "CPU", "memory" to "内存", "disk_io" to "磁盘IO").forEach { (key, label) ->
                        val isSelected = processType == key
                        Box(
                            modifier = Modifier.height(26.dp)
                                .background(if (isSelected) Color(0xFF18181B) else Color(0xFFF3F4F6), RoundedCornerShape(13.dp))
                                .clickable { onTypeChange(key) }.padding(horizontal = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSelected) Color.White else Color(0xFF71717A))
                        }
                    }
                }
                Text("Top 进程", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF71717A), letterSpacing = 1.sp)
                if (topProcesses.isEmpty()) {
                    Text(if (panelId.isEmpty()) "请先选择面板" else "加载中...", fontSize = 13.sp, color = Color(0xFFA1A1AA))
                } else {
                    topProcesses.forEach { proc ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.weight(1f)) {
                                Text(proc.name, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Color(0xFF18181B))
                                Text("PID: ${proc.pid}", fontSize = 12.sp, color = Color(0xFFA1A1AA))
                            }
                            Text("${proc.value.fmt1()}%", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF2563EB))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProcessTab(
    processes: List<ProcessApiItem>,
    total: Long,
    isLoading: Boolean,
    sort: String,
    onSortChange: (String) -> Unit,
    onKill: (Int) -> Unit,
    onSignal: (Int, Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("排序:", fontSize = 12.sp, color = Color(0xFF71717A))
            listOf("cpu" to "CPU", "rss" to "内存", "pid" to "PID", "name" to "名称").forEach { (key, label) ->
                val isSelected = sort == key
                Box(
                    modifier = Modifier.height(26.dp)
                        .background(if (isSelected) Color(0xFF2563EB) else Color(0xFFF3F4F6), RoundedCornerShape(6.dp))
                        .clickable { onSortChange(key) }.padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(label, fontSize = 11.sp, color = if (isSelected) Color.White else Color(0xFF71717A))
                }
            }
            if (isLoading) CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = Color(0xFF2563EB))
        }

        if (processes.isEmpty() && !isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("暂无进程数据，点击右上角 ↻ 刷新", fontSize = 14.sp, color = Color(0xFFA1A1AA))
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)) {
                items(processes.size) { index ->
                    val proc = processes[index]
                    ProcessRow(proc = proc, onKill = { onKill(proc.pid) }, onSignal = { sig -> onSignal(proc.pid, sig) })
                    if (index < processes.lastIndex) HorizontalDivider(thickness = 0.5.dp, color = Color(0xFFF0F0F0))
                }
            }
        }
    }
}

@Composable
private fun ProcessRow(proc: ProcessApiItem, onKill: () -> Unit, onSignal: (Int) -> Unit) {
    var showActions by remember { mutableStateOf(false) }
    var showSignalPicker by remember { mutableStateOf(false) }

    if (showSignalPicker) {
        SignalPickerDialog(
            onDismiss = { showSignalPicker = false },
            onSelect = { sig -> showSignalPicker = false; showActions = false; onSignal(sig) }
        )
    }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(proc.name, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF18181B))
                Text("PID: ${proc.pid}  用户: ${proc.username}", fontSize = 11.sp, color = Color(0xFF71717A))
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("CPU ${proc.cpu.fmt1()}%", fontSize = 12.sp, color = Color(0xFF2563EB))
                Text("MEM ${formatMem(proc.rss)}", fontSize = 12.sp, color = Color(0xFF059669))
            }
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier.size(28.dp).clip(RoundedCornerShape(6.dp))
                    .background(if (showActions) Color(0xFFEF4444) else Color(0xFFF3F4F6))
                    .clickable { showActions = !showActions },
                contentAlignment = Alignment.Center
            ) { Text("⋮", fontSize = 14.sp, color = if (showActions) Color.White else Color(0xFF71717A)) }
        }

        if (showActions) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ActionChip("结束(SIGKILL)", Color(0xFFEF4444)) {
                    showActions = false; onKill()
                }
                ActionChip("发送信号", Color(0xFF7C3AED)) {
                    showSignalPicker = true
                }
                ActionChip("收起", Color(0xFF71717A)) {
                    showActions = false
                }
            }
            Spacer(Modifier.height(6.dp))
        }
    }
}

@Composable
private fun ActionChip(label: String, color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier.height(26.dp).clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.1f))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
            .clickable(onClick = onClick).padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) { Text(label, fontSize = 11.sp, color = color, fontWeight = FontWeight.Medium) }
}

@Composable
private fun SignalPickerDialog(onDismiss: () -> Unit, onSelect: (Int) -> Unit) {
    val signals = listOf(
        1 to "SIGHUP (1) - 重载配置",
        2 to "SIGINT (2) - 中断",
        9 to "SIGKILL (9) - 强制结束",
        10 to "SIGUSR1 (10) - 用户信号1",
        12 to "SIGUSR2 (12) - 用户信号2",
        15 to "SIGTERM (15) - 优雅结束",
        18 to "SIGCONT (18) - 继续",
        19 to "SIGSTOP (19) - 暂停"
    )
    AnimatedAppDialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                .background(Color.White).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text("发送信号", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF18181B))
            Spacer(Modifier.height(8.dp))
            signals.forEach { (sig, label) ->
                Box(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF9FAFB)).clickable { onSelect(sig) }.padding(12.dp)
                ) { Text(label, fontSize = 13.sp, color = Color(0xFF18181B)) }
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                Text("取消", color = Color(0xFF71717A))
            }
        }
    }
}

@Composable
private fun HistoryTab(
    history: MonitorHistoryResponse?,
    isLoading: Boolean
) {
    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFF2563EB))
        }
        return
    }

    if (history == null || history.times.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("暂无历史数据，点击 ↻ 加载", fontSize = 14.sp, color = Color(0xFFA1A1AA))
        }
        return
    }

    val cpuValues = history.cpu.percent.mapNotNull { it.toDoubleOrNull() }
    val memTotal = history.mem.total.toDoubleOrNull() ?: 0.0
    val memValues = if (memTotal > 0.0) {
        history.mem.used.mapNotNull { used ->
            used.toDoubleOrNull()?.let { it / memTotal * 100.0 }
        }
    } else {
        emptyList()
    }
    val swapTotal = history.swap.total.toDoubleOrNull() ?: 0.0
    val swapValues = if (swapTotal > 0.0) {
        history.swap.used.mapNotNull { used ->
            used.toDoubleOrNull()?.let { it / swapTotal * 100.0 }
        }
    } else {
        emptyList()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            HistoryChart(
                title = "CPU 使用率",
                values = cpuValues,
                color = Color(0xFF2563EB),
                bgColor = Color(0xFFEFF6FF),
                unit = "%"
            )
        }
        item {
            HistoryChart(
                title = "内存使用率",
                values = memValues,
                color = Color(0xFF059669),
                bgColor = Color(0xFFECFDF5),
                unit = "%"
            )
        }
        if (swapValues.isNotEmpty()) {
            item {
                HistoryChart(
                    title = "Swap 使用率",
                    values = swapValues,
                    color = Color(0xFFD97706),
                    bgColor = Color(0xFFFEF3C7),
                    unit = "%"
                )
            }
        }
        item {
            // 时间范围信息
            val count = history.times.size
            if (count > 0) {
                Column(
                    modifier = Modifier.fillMaxWidth().background(Color(0xFFF3F4F6), RoundedCornerShape(12.dp)).padding(12.dp)
                ) {
                    Text("共 $count 条记录", fontSize = 12.sp, color = Color(0xFF71717A))
                }
            }
        }
    }
}

@Composable
private fun HistoryChart(
    title: String,
    values: List<Double>,
    color: Color,
    bgColor: Color,
    unit: String
) {
    if (values.isEmpty()) return

    val max = values.maxOrNull()?.coerceAtLeast(1.0) ?: 100.0
    val min = values.minOrNull() ?: 0.0
    val avg = values.average()
    val last = values.lastOrNull() ?: 0.0

    Column(
        modifier = Modifier.fillMaxWidth().background(bgColor, RoundedCornerShape(16.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = color)
            Text("当前: ${last.fmt1()}$unit", fontSize = 13.sp, color = color, fontWeight = FontWeight.Bold)
        }

        // 迷你柱状图
        val displayCount = minOf(values.size, 60)
        val step = values.size / displayCount
        val displayValues = (0 until displayCount).map { i -> values[i * step] }

        Row(
            modifier = Modifier.fillMaxWidth().height(48.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            displayValues.forEach { v ->
                val fraction = ((v / max) * 48).toInt().coerceIn(2, 48)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(fraction.dp)
                        .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                        .background(color.copy(alpha = 0.7f))
                )
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("最小: ${min.fmt1()}$unit", fontSize = 11.sp, color = color.copy(alpha = 0.7f))
            Text("平均: ${avg.fmt1()}$unit", fontSize = 11.sp, color = color.copy(alpha = 0.7f))
            Text("最大: ${max.fmt1()}$unit", fontSize = 11.sp, color = color.copy(alpha = 0.7f))
        }
    }
}

@Composable
private fun MonitorSettingDialog(
    setting: MonitorSetting?,
    onDismiss: () -> Unit,
    onSave: (Boolean, Int, Int) -> Unit,
    onClear: () -> Unit
) {
    var enabled by remember(setting) { mutableStateOf(setting?.enabled ?: true) }
    var daysText by remember(setting) { mutableStateOf(setting?.days?.toString() ?: "30") }
    var intervalText by remember(setting) { mutableStateOf(setting?.interval?.toString() ?: "5") }

    AnimatedAppDialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                .background(Color.White).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("监控设置", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF18181B))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("启用历史记录", fontSize = 13.sp, color = Color(0xFF18181B))
                Box(
                    modifier = Modifier.width(44.dp).height(26.dp)
                        .background(if (enabled) Color(0xFF2563EB) else Color(0xFFE4E4E7), RoundedCornerShape(13.dp))
                        .clickable { enabled = !enabled }.padding(3.dp),
                    contentAlignment = if (enabled) Alignment.CenterEnd else Alignment.CenterStart
                ) { Box(Modifier.size(20.dp).background(Color.White, RoundedCornerShape(10.dp))) }
            }

            OutlinedTextField(
                value = daysText, onValueChange = { daysText = it },
                label = { Text("保留天数（0=永久）") }, modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            OutlinedTextField(
                value = intervalText, onValueChange = { intervalText = it },
                label = { Text("采集间隔（分钟，1-120）") }, modifier = Modifier.fillMaxWidth(), singleLine = true
            )

            HorizontalDivider(color = Color(0xFFE4E4E7))

            Box(
                modifier = Modifier.fillMaxWidth().height(36.dp).clip(RoundedCornerShape(8.dp))
                    .border(1.dp, Color(0xFFEF4444), RoundedCornerShape(8.dp))
                    .clickable { onClear(); onDismiss() },
                contentAlignment = Alignment.Center
            ) { Text("清空历史数据", fontSize = 13.sp, color = Color(0xFFEF4444)) }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("取消", color = Color(0xFF71717A)) }
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Color(0xFF2563EB))
                        .clickable {
                            onSave(enabled, daysText.toIntOrNull() ?: 30, intervalText.toIntOrNull() ?: 5)
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) { Text("保存", fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Medium) }
            }
        }
    }
}

@Composable
private fun MonitorStatCard(label: String, valueText: String, fraction: Float, bgColor: Color, barColor: Color, barBgColor: Color, textColor: Color) {
    Column(
        modifier = Modifier.fillMaxWidth().background(bgColor, RoundedCornerShape(16.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = textColor)
        Text(valueText, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = textColor)
        Box(modifier = Modifier.fillMaxWidth().height(6.dp).background(barBgColor, RoundedCornerShape(3.dp))) {
            Box(modifier = Modifier.fillMaxWidth(fraction).fillMaxHeight().background(barColor, RoundedCornerShape(3.dp)))
        }
    }
}

@Composable
private fun NetSpeedRow(arrow: String, label: String, speedBps: Long, color: Color) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(arrow, fontSize = 18.sp, color = color)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF71717A))
            Text(formatSpeed(speedBps), fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF18181B))
        }
    }
}

private fun formatSpeed(bps: Long): String = when {
    bps >= 1024 * 1024 * 1024 -> "${(bps / (1024.0 * 1024 * 1024)).fmt2()} GB/s"
    bps >= 1024 * 1024 -> "${(bps / (1024.0 * 1024)).fmt2()} MB/s"
    bps >= 1024 -> "${(bps / 1024.0).fmt2()} KB/s"
    else -> "$bps B/s"
}

private fun formatMem(rss: Long): String = when {
    rss >= 1024 * 1024 -> "${rss / (1024 * 1024)} MB"
    rss >= 1024 -> "${rss / 1024} KB"
    else -> "$rss B"
}

private fun Double.fmt1(): String { val v = (this * 10).toLong(); return "${v / 10}.${v % 10}" }
private fun Double.fmt2(): String { val v = (this * 100).toLong(); return "${v / 100}.${(v % 100).toString().padStart(2, '0')}" }
