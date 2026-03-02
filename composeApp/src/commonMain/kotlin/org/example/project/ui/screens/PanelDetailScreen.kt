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
import org.example.project.ui.components.IconPlaceholder
import org.example.project.ui.components.StatusBarSpacer
import org.example.project.ui.components.MenuItem
import org.example.project.viewmodel.PanelDetailViewModel

@Composable
fun PanelDetailScreen(
    panelId: String,
    onBackClick: () -> Unit = {},
    onMenuClick: (String) -> Unit = {}
) {
    val vm: PanelDetailViewModel = viewModel()
    val config by vm.config.collectAsStateWithLifecycle()
    val systemInfo by vm.systemInfo.collectAsStateWithLifecycle()
    val currentUsage by vm.currentUsage.collectAsStateWithLifecycle()
    val isLoading by vm.isLoading.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()

    LaunchedEffect(panelId) {
        vm.init(panelId)
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
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.5.dp, Color(0xFFE4E4E7), RoundedCornerShape(10.dp))
                    .clickable(onClick = onBackClick),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "‹", fontSize = 20.sp, color = Color(0xFF18181B))
            }
            Text(
                text = config?.name ?: "面板详情",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF18181B)
            )
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.5.dp, Color(0xFFE4E4E7), RoundedCornerShape(10.dp))
                    .clickable { vm.refresh() },
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = Color(0xFF2563EB)
                    )
                } else {
                    Text(text = "↻", fontSize = 18.sp, color = Color(0xFF18181B))
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // 错误提示
            if (error != null) {
                item {
                    val hint = when {
                        error!!.contains("无效的请求 IP") || error!!.contains("invalid request ip") ->
                            "\n提示：当前设备 IP 不在该 Token 的 IP 白名单中，请前往「面板→API 密钥」更新允许的 IP"
                        error!!.contains("IP") && error!!.contains("绑定") ->
                            "\n提示：当前设备 IP 不在面板 IP 绑定列表中，请在「面板设置→安全设置」中更新"
                        error!!.contains("418") ->
                            "\n提示：请检查安全入口路径是否正确，或面板 IP/域名绑定设置"
                        else -> ""
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 8.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFFEF2F2))
                            .padding(12.dp)
                    ) {
                        Text(text = "⚠ $error$hint", fontSize = 13.sp, color = Color(0xFFEF4444))
                    }
                }
            }

            // 服务器状态卡片
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.5.dp, Color(0xFFE4E4E7), RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "服务器状态",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF71717A),
                            letterSpacing = 1.sp
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "${config?.host ?: ""}:${config?.port ?: ""}",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF18181B)
                                )
                                if (systemInfo != null) {
                                    Text(
                                        text = buildString {
                                            if (systemInfo!!.hostname.isNotEmpty()) append(systemInfo!!.hostname)
                                            if (systemInfo!!.os_name.isNotEmpty()) {
                                                if (isNotEmpty()) append(" · ")
                                                append(systemInfo!!.os_name)
                                            }
                                            if (systemInfo!!.uptime > 0) {
                                                if (isNotEmpty()) append(" · 运行 ")
                                                append(formatUptime(systemInfo!!.uptime))
                                            }
                                        },
                                        fontSize = 13.sp,
                                        color = Color(0xFF71717A)
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .height(24.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        if (systemInfo != null) Color(0xFFECFDF5) else Color(0xFFF4F4F5)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (systemInfo != null) "在线" else "连接中",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (systemInfo != null) Color(0xFF10B981) else Color(0xFFA1A1AA)
                                )
                            }
                        }
                    }
                }
            }

            // 资源使用三卡
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "资源使用",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF71717A),
                        letterSpacing = 1.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val cpu = currentUsage?.percent ?: 0.0
                        val mem = currentUsage?.mem?.usedPercent ?: 0.0
                        val disk = currentUsage?.disk_usage?.maxOfOrNull { it.usedPercent } ?: 0.0
                        ResourceStatCard(
                            value = "${cpu.toInt()}%",
                            label = "CPU",
                            modifier = Modifier.weight(1f)
                        )
                        ResourceStatCard(
                            value = "${mem.toInt()}%",
                            label = "内存",
                            modifier = Modifier.weight(1f)
                        )
                        ResourceStatCard(
                            value = "${disk.toInt()}%",
                            label = "磁盘",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    // 负载
                    currentUsage?.load?.let { load ->
                        Text(
                            text = "系统负载 ${load.load1.fmt2()} / ${load.load5.fmt2()} / ${load.load15.fmt2()}",
                            fontSize = 12.sp,
                            color = Color(0xFFA1A1AA)
                        )
                    }
                }
            }

            // 功能菜单
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "功能管理",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF71717A),
                        letterSpacing = 1.sp
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.5.dp, Color(0xFFE4E4E7), RoundedCornerShape(16.dp))
                    ) {
                        Column {
                            val menuItems = listOf(
                                "websites" to "网站管理",
                                "databases" to "数据库",
                                "db_servers" to "数据库服务器",
                                "db_users" to "数据库用户",
                                "projects" to "项目管理",
                                "files" to "文件管理",
                                "terminal" to "终端",
                                "firewall" to "防火墙",
                                "ssl" to "SSL 证书",
                                "settings" to "面板设置",
                                "software" to "软件商店",
                                "monitor" to "系统监控",
                                "logs" to "日志查看",
                                "cron" to "计划任务",
                                "tasks" to "任务中心",
                                "backup" to "备份管理",
                                "backup_storage" to "备份存储",
                                "ssh" to "SSH 主机",
                                "webhooks" to "WebHook",
                                "tokens" to "Token 管理",
                                "users" to "用户管理",
                                "redis_keys" to "Redis 键管理",
                                "cert_config" to "ACME/DNS 配置",
                                "website_stat" to "网站统计",
                                "firewall_scan" to "防火墙扫描审计",
                                "config" to "配置编辑"
                            )
                            menuItems.forEachIndexed { index, (key, label) ->
                                MenuItem(
                                    icon = { IconPlaceholder() },
                                    label = label,
                                    onClick = { onMenuClick(key) }
                                )
                                if (index < menuItems.lastIndex) {
                                    HorizontalDivider(thickness = 1.dp, color = Color(0xFFE4E4E7))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResourceStatCard(value: String, label: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(80.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(1.5.dp, Color(0xFFE4E4E7), RoundedCornerShape(16.dp))
            .background(Color.White)
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = value,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF18181B)
            )
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF71717A)
            )
        }
    }
}

/** 保留两位小数的简单格式化（KMP 兼容） */
private fun Double.fmt2(): String {
    val v = (this * 100).toLong()
    return "${v / 100}.${(v % 100).toString().padStart(2, '0')}"
}

private fun formatUptime(seconds: Long): String {
    val days = seconds / 86400
    val hours = (seconds % 86400) / 3600
    return if (days > 0) "${days}天" else "${hours}小时"
}
