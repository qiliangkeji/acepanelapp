package com.acepanel.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.acepanel.app.data.PanelConfig
import com.acepanel.app.data.PanelRuntimeStatus

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun PanelCard(
    config: PanelConfig,
    status: PanelRuntimeStatus = PanelRuntimeStatus(),
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.5.dp, Color(0xFFE4E4E7), RoundedCornerShape(16.dp))
            .background(Color.White)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Column {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = config.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF18181B)
                    )
                    Text(
                        text = "${config.host}:${config.port}",
                        fontSize = 12.sp,
                        color = Color(0xFF71717A)
                    )
                }
                OnlineBadge(isOnline = status.isOnline)
            }

            if (status.isOnline) {
                HorizontalDivider(thickness = 1.dp, color = Color(0xFFE4E4E7))

                // 系统信息行
                if (status.osName.isNotEmpty() || status.uptime > 0) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = status.osName.ifEmpty { "未知系统" },
                            fontSize = 12.sp,
                            color = Color(0xFF71717A)
                        )
                        if (status.uptime > 0) {
                            Text(
                                text = "已运行 ${formatUptime(status.uptime)}",
                                fontSize = 12.sp,
                                color = Color(0xFFA1A1AA)
                            )
                        }
                    }
                }

                // 资源使用率
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ResourceBar(label = "CPU", percent = status.cpuPercent)
                    ResourceBar(label = "内存", percent = status.memPercent)
                    ResourceBar(label = "磁盘", percent = status.diskPercent)
                }
            }
        }
    }
}

@Composable
private fun ResourceBar(label: String, percent: Double) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = Color(0xFF71717A),
            modifier = Modifier.width(28.dp)
        )
        LinearProgressIndicator(
            progress = { (percent / 100.0).toFloat().coerceIn(0f, 1f) },
            modifier = Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(2.dp)),
            color = when {
                percent >= 90 -> Color(0xFFEF4444)
                percent >= 70 -> Color(0xFFF59E0B)
                else -> Color(0xFF2563EB)
            },
            trackColor = Color(0xFFF4F4F5)
        )
        Text(
            text = "${percent.toInt()}%",
            fontSize = 11.sp,
            color = Color(0xFF71717A),
            modifier = Modifier.width(32.dp)
        )
    }
}

@Composable
fun OnlineBadge(isOnline: Boolean) {
    val (bgColor, textColor, text) = if (isOnline) {
        Triple(Color(0xFFECFDF5), Color(0xFF10B981), "在线")
    } else {
        Triple(Color(0xFFF4F4F5), Color(0xFFA1A1AA), "离线")
    }
    Box(
        modifier = Modifier
            .height(24.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = textColor)
    }
}

@Composable
fun StatusBadge(isOnline: Boolean) = OnlineBadge(isOnline)

private fun formatUptime(seconds: Long): String {
    val days = seconds / 86400
    val hours = (seconds % 86400) / 3600
    return if (days > 0) "${days}天${hours}小时" else "${hours}小时"
}
