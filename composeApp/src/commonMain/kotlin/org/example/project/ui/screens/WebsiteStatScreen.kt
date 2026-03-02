package org.example.project.ui.screens

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
import org.example.project.data.StatTotals
import org.example.project.ui.components.StatusBarSpacer
import org.example.project.viewmodel.WebsiteStatViewModel

@Composable
fun WebsiteStatScreen(
    panelId: String,
    onBack: () -> Unit = {}
) {
    val vm: WebsiteStatViewModel = viewModel()
    val overview by vm.overview.collectAsStateWithLifecycle()
    val setting by vm.setting.collectAsStateWithLifecycle()
    val realtime by vm.realtime.collectAsStateWithLifecycle()
    val siteItems by vm.siteItems.collectAsStateWithLifecycle()
    val spiderItems by vm.spiderItems.collectAsStateWithLifecycle()
    val clientStats by vm.clientStats.collectAsStateWithLifecycle()
    val ipItems by vm.ipItems.collectAsStateWithLifecycle()
    val geoItems by vm.geoItems.collectAsStateWithLifecycle()
    val uriItems by vm.uriItems.collectAsStateWithLifecycle()
    val slowUriItems by vm.slowUriItems.collectAsStateWithLifecycle()
    val errorItems by vm.errorItems.collectAsStateWithLifecycle()
    val isLoading by vm.isLoading.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    val actionError by vm.actionError.collectAsStateWithLifecycle()
    val startDate by vm.startDate.collectAsStateWithLifecycle()
    val endDate by vm.endDate.collectAsStateWithLifecycle()

    LaunchedEffect(panelId) { vm.init(panelId) }

    var showClearConfirm by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
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
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.5.dp, Color(0xFFE4E4E7), RoundedCornerShape(10.dp))
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) { Text("‹", fontSize = 20.sp, color = Color(0xFF18181B)) }
            Text("网站统计", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF18181B))
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

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            // 日期范围选择
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = startDate,
                        onValueChange = { vm.startDate.value = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("开始日期", fontSize = 12.sp) },
                        placeholder = { Text("YYYY-MM-DD", fontSize = 12.sp, color = Color(0xFFA1A1AA)) },
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
                        label = { Text("结束日期", fontSize = 12.sp) },
                        placeholder = { Text("YYYY-MM-DD", fontSize = 12.sp, color = Color(0xFFA1A1AA)) },
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
                            .clickable { vm.loadOverview() }
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("查询", fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Medium)
                    }
                }
            }

            // 错误提示
            if (error != null || actionError != null) {
                item {
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
            }

            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator(color = Color(0xFF2563EB)) }
                }
            } else if (overview == null) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("暂无统计数据", fontSize = 14.sp, color = Color(0xFFA1A1AA))
                    }
                }
            } else {
                val curr = overview!!.current
                val prev = overview!!.previous

                // 概览指标卡
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatSectionTitle("流量概览")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            StatMetricCard("PV", curr.pv, prev.pv, Modifier.weight(1f))
                            StatMetricCard("UV", curr.uv, prev.uv, Modifier.weight(1f))
                            StatMetricCard("IP数", curr.ip, prev.ip, Modifier.weight(1f))
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            StatMetricCard("请求数", curr.requests, prev.requests, Modifier.weight(1f))
                            StatMetricCard("错误数", curr.errors, prev.errors, Modifier.weight(1f))
                            StatMetricCard("爬虫", curr.spiders, prev.spiders, Modifier.weight(1f))
                        }
                    }
                }

                // 状态码分布
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatSectionTitle("状态码分布（本期）")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            StatStatusCard("2xx", curr.status_2xx, Color(0xFF10B981), Color(0xFFECFDF5), Modifier.weight(1f))
                            StatStatusCard("3xx", curr.status_3xx, Color(0xFF3B82F6), Color(0xFFEFF6FF), Modifier.weight(1f))
                            StatStatusCard("4xx", curr.status_4xx, Color(0xFFF59E0B), Color(0xFFFFFBEB), Modifier.weight(1f))
                            StatStatusCard("5xx", curr.status_5xx, Color(0xFFEF4444), Color(0xFFFEF2F2), Modifier.weight(1f))
                        }
                    }
                }

                // 带宽
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.5.dp, Color(0xFFE4E4E7), RoundedCornerShape(12.dp))
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("出站带宽", fontSize = 12.sp, color = Color(0xFF71717A))
                                Text(
                                    formatBytes(curr.bandwidth),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF18181B)
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("上期", fontSize = 12.sp, color = Color(0xFF71717A))
                                Text(
                                    formatBytes(prev.bandwidth),
                                    fontSize = 14.sp,
                                    color = Color(0xFFA1A1AA)
                                )
                            }
                        }
                    }
                }

                // 统计设置
                if (setting != null) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 4.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.5.dp, Color(0xFFE4E4E7), RoundedCornerShape(12.dp))
                                .padding(16.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("统计设置", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF71717A))
                                Text(
                                    "数据保留 ${setting!!.days} 天  •  Body 追踪: ${if (setting!!.body_enabled) "开" else "关"}",
                                    fontSize = 13.sp,
                                    color = Color(0xFF18181B)
                                )
                            }
                        }
                    }
                }

                if (realtime != null) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            StatSectionTitle("实时指标")
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                StatMetricCard("RPS", realtime!!.rps.toLong(), 0, Modifier.weight(1f))
                                StatMetricCard("出站B/s", realtime!!.bandwidth.toLong(), 0, Modifier.weight(1f))
                                StatMetricCard("入站B/s", realtime!!.bandwidth_in.toLong(), 0, Modifier.weight(1f))
                            }
                        }
                    }
                }

                if (siteItems.isNotEmpty()) {
                    item {
                        StatSimpleListSection(
                            title = "站点排行（请求数）",
                            rows = siteItems.take(6).map { "${it.site} · ${it.requests}" }
                        )
                    }
                }
                if (spiderItems.isNotEmpty()) {
                    item {
                        StatSimpleListSection(
                            title = "蜘蛛排行",
                            rows = spiderItems.take(6).map { "${it.spider} · ${it.requests}" }
                        )
                    }
                }
                if (clientStats != null && clientStats!!.browsers.isNotEmpty()) {
                    item {
                        StatSimpleListSection(
                            title = "客户端浏览器排行",
                            rows = clientStats!!.browsers.take(6).map { "${it.name} · ${it.requests}" }
                        )
                    }
                }
                if (ipItems.isNotEmpty()) {
                    item {
                        StatSimpleListSection(
                            title = "来源 IP 排行",
                            rows = ipItems.take(6).map { "${it.ip} · ${it.requests}" }
                        )
                    }
                }
                if (geoItems.isNotEmpty()) {
                    item {
                        StatSimpleListSection(
                            title = "地域统计",
                            rows = geoItems.take(6).map { "${it.country.ifEmpty { "未知" }} ${it.region} · ${it.requests}" }
                        )
                    }
                }
                if (uriItems.isNotEmpty()) {
                    item {
                        StatSimpleListSection(
                            title = "URI 排行",
                            rows = uriItems.take(6).map { "${it.uri.take(42)} · ${it.requests}" }
                        )
                    }
                }
                if (slowUriItems.isNotEmpty()) {
                    item {
                        StatSimpleListSection(
                            title = "慢 URI 排行",
                            rows = slowUriItems.take(6).map {
                                val avg = if (it.request_time_count > 0) it.request_time_sum / it.request_time_count else 0
                                "${it.uri.take(38)} · ${avg}ms"
                            }
                        )
                    }
                }
                if (errorItems.isNotEmpty()) {
                    item {
                        StatSimpleListSection(
                            title = "错误日志",
                            rows = errorItems.take(6).map { "${it.status} ${it.method} ${it.uri.take(36)}" }
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }

    // 清空确认
    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("清空统计数据", fontWeight = FontWeight.SemiBold) },
            text = { Text("此操作将永久删除所有网站统计记录，不可恢复。确认继续？") },
            confirmButton = {
                TextButton(onClick = { vm.clearStats(); showClearConfirm = false }) {
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
private fun StatSectionTitle(text: String) {
    Text(
        text,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color(0xFF71717A),
        letterSpacing = 1.sp
    )
}

@Composable
private fun StatMetricCard(label: String, current: Long, previous: Long, modifier: Modifier = Modifier) {
    val diff = if (previous > 0) ((current - previous).toDouble() / previous * 100).toInt() else 0
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .border(1.5.dp, Color(0xFFE4E4E7), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, fontSize = 11.sp, color = Color(0xFF71717A))
            Text(
                formatCompact(current),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF18181B)
            )
            if (previous > 0) {
                val color = if (diff >= 0) Color(0xFF10B981) else Color(0xFFEF4444)
                val sign = if (diff >= 0) "+" else ""
                Text("$sign${diff}%", fontSize = 11.sp, color = color, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun StatStatusCard(
    label: String, value: Long,
    textColor: Color, bgColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = textColor)
            Text(formatCompact(value), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textColor)
        }
    }
}

@Composable
private fun StatSimpleListSection(title: String, rows: List<String>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatSectionTitle(title)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(1.5.dp, Color(0xFFE4E4E7), RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                rows.forEachIndexed { index, row ->
                    Text("${index + 1}. $row", fontSize = 12.sp, color = Color(0xFF18181B))
                }
            }
        }
    }
}

private fun formatCompact(n: Long): String = when {
    n >= 1_000_000 -> "${n / 1_000_000}M"
    n >= 1_000 -> "${n / 1_000}K"
    else -> n.toString()
}

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1_073_741_824L -> "${bytes / 1_073_741_824}GB"
    bytes >= 1_048_576L -> "${bytes / 1_048_576}MB"
    bytes >= 1_024L -> "${bytes / 1_024}KB"
    else -> "${bytes}B"
}
