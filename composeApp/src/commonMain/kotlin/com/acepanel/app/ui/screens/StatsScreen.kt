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
import com.acepanel.app.ui.components.StatusBarSpacer
import com.acepanel.app.viewmodel.StatsViewModel

@Composable
fun StatsScreen(onBack: () -> Unit) {
    val vm = viewModel<StatsViewModel>()
    val panelStats by vm.panelStats.collectAsStateWithLifecycle()
    val isLoading by vm.isLoading.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { vm.load() }

    val onlineCount = panelStats.count { it.isOnline }
    val offlineCount = panelStats.size - onlineCount
    val avgCpu = if (panelStats.isEmpty()) 0.0 else panelStats.filter { it.isOnline }.map { it.cpuPercent }.average().let { if (it.isNaN()) 0.0 else it }
    val avgMem = if (panelStats.isEmpty()) 0.0 else panelStats.filter { it.isOnline }.map { it.memPercent }.average().let { if (it.isNaN()) 0.0 else it }
    val avgDisk = if (panelStats.isEmpty()) 0.0 else panelStats.filter { it.isOnline }.map { it.diskPercent }.average().let { if (it.isNaN()) 0.0 else it }
    val totalWebsites = panelStats.sumOf { it.websiteCount }

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
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "统计概览",
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF18181B)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color(0xFF2563EB))
                }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.5.dp, Color(0xFFE4E4E7), RoundedCornerShape(10.dp))
                        .clickable { vm.load() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "↻", fontSize = 16.sp, color = Color(0xFF18181B))
                }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.5.dp, Color(0xFFE4E4E7), RoundedCornerShape(10.dp))
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "‹", fontSize = 20.sp, color = Color(0xFF18181B))
                }
            }
        }

        if (panelStats.isEmpty() && !isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("暂无面板数据", fontSize = 14.sp, color = Color(0xFFA1A1AA))
            }
            return@Column
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            // Server Overview
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("服务器概览", fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF71717A), letterSpacing = 1.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatsOverviewCard(panelStats.size.toString(), "总面板", Color(0xFFEFF6FF), Modifier.weight(1f))
                        StatsOverviewCard(onlineCount.toString(), "在线", Color(0xFFECFDF5), Modifier.weight(1f))
                        StatsOverviewCard(offlineCount.toString(), "离线", Color(0xFFFEF3C7), Modifier.weight(1f))
                    }
                }
            }

            // Average Resource
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("平均资源（在线面板）", fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF71717A), letterSpacing = 1.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatsMiniCard("${avgCpu.toInt()}%", "CPU", Modifier.weight(1f))
                        StatsMiniCard("${avgMem.toInt()}%", "内存", Modifier.weight(1f))
                        StatsMiniCard("${avgDisk.toInt()}%", "磁盘", Modifier.weight(1f))
                    }
                }
            }

            // Summary row
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.5.dp, Color(0xFFE4E4E7), RoundedCornerShape(12.dp))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(totalWebsites.toString(), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF18181B))
                            Text("总网站", fontSize = 12.sp, color = Color(0xFF71717A))
                        }
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.5.dp, Color(0xFFE4E4E7), RoundedCornerShape(12.dp))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(panelStats.sumOf { it.dbCount }.toString(), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF18181B))
                            Text("总数据库", fontSize = 12.sp, color = Color(0xFF71717A))
                        }
                    }
                }
            }

            // Per-panel list
            item {
                Text(
                    text = "面板详情",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF71717A),
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )
            }

            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.5.dp, Color(0xFFE4E4E7), RoundedCornerShape(16.dp))
                ) {
                    Column {
                        panelStats.forEachIndexed { index, stat ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(if (stat.isOnline) Color(0xFF4ADE80) else Color(0xFFEF4444))
                                    )
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(stat.config.name, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF18181B))
                                        Text("${stat.config.host}:${stat.config.port}", fontSize = 12.sp, color = Color(0xFF71717A))
                                    }
                                }
                                if (stat.isOnline) {
                                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text("CPU ${stat.cpuPercent.toInt()}%", fontSize = 12.sp, color = Color(0xFF2563EB))
                                        Text("MEM ${stat.memPercent.toInt()}%", fontSize = 12.sp, color = Color(0xFF059669))
                                    }
                                } else {
                                    Text("离线", fontSize = 12.sp, color = Color(0xFFEF4444))
                                }
                            }
                            if (index < panelStats.lastIndex) {
                                HorizontalDivider(thickness = 0.5.dp, color = Color(0xFFF0F0F0))
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun StatsOverviewCard(value: String, label: String, bgColor: Color, modifier: Modifier) {
    Box(
        modifier = modifier
            .height(90.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(value, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFF18181B))
            Text(label, fontSize = 12.sp, color = Color(0xFF71717A))
        }
    }
}

@Composable
private fun StatsMiniCard(value: String, label: String, modifier: Modifier) {
    Box(
        modifier = modifier
            .height(72.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(1.5.dp, Color(0xFFE4E4E7), RoundedCornerShape(12.dp))
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF18181B))
            Text(label, fontSize = 11.sp, color = Color(0xFF71717A))
        }
    }
}
