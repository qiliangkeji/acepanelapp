package com.acepanel.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.acepanel.app.data.WebsiteDetailData
import com.acepanel.app.ui.components.MenuItem
import com.acepanel.app.ui.components.MenuSvgIcon
import com.acepanel.app.ui.components.StatusBarSpacer
import com.acepanel.app.ui.components.websiteFeatureIcon
import com.acepanel.app.viewmodel.WebsiteDetailViewModel

@Composable
fun WebsiteDetailScreen(
    panelId: String = "",
    websiteId: Long = 0,
    onBackClick: () -> Unit = {},
    onMenuClick: (String, WebsiteDetailData?, String) -> Unit = { _, _, _ -> }
) {
    val vm = viewModel<WebsiteDetailViewModel>()
    val website by vm.website.collectAsStateWithLifecycle()
    val isLoading by vm.isLoading.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    val actionError by vm.actionError.collectAsStateWithLifecycle()
    val isActing by vm.isActing.collectAsStateWithLifecycle()
    val webserver by vm.webserver.collectAsStateWithLifecycle()

    LaunchedEffect(panelId, websiteId) {
        if (panelId.isNotEmpty() && websiteId > 0) vm.init(panelId, websiteId)
    }

    val title = website?.domains?.firstOrNull() ?: website?.name ?: "网站详情"
    val isRunning = website != null  // status is part of the full config; we reload after status change

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
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF18181B)
            )
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.5.dp, Color(0xFFE4E4E7), RoundedCornerShape(10.dp))
                    .clickable { vm.load() },
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color(0xFF2563EB))
                } else {
                    Text(text = "↻", fontSize = 18.sp, color = Color(0xFF18181B))
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

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // 网站信息卡片
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.5.dp, Color(0xFFE4E4E7), RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "网站信息",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF71717A),
                                letterSpacing = 1.sp
                            )
                            if (website != null) {
                                val typeLabel = when (website!!.type) {
                                    "php" -> "PHP ${website!!.php}"
                                    "proxy" -> "反向代理"
                                    "static" -> "静态"
                                    else -> website!!.type
                                }
                                Box(
                                    modifier = Modifier
                                        .height(22.dp)
                                        .background(Color(0xFFEFF6FF), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(typeLabel, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color(0xFF2563EB))
                                }
                            }
                        }
                        if (website != null) {
                            // 域名列表
                            if (website!!.domains.isNotEmpty()) {
                                DetailRow("域名", website!!.domains.joinToString(", "))
                            }
                            // 监听端口
                            val listens = website!!.listens.joinToString(", ") { it.address }
                            if (listens.isNotEmpty()) DetailRow("监听", listens)
                            // 目录
                            if (website!!.path.isNotEmpty()) DetailRow("目录", website!!.path)
                            // SSL
                            if (website!!.ssl) {
                                val sslExpiry = website!!.ssl_not_after.take(10)
                                DetailRow("SSL", if (sslExpiry.isNotEmpty()) "已启用（到期 $sslExpiry）" else "已启用")
                            }
                        } else if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        }
                    }
                }
            }

            // 操作按钮
            if (website != null) {
                item {
                    if (actionError != null) {
                        Text(
                            text = actionError!!,
                            fontSize = 12.sp,
                            color = Color(0xFFEF4444),
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ActionBtn(
                            text = "启动", bgColor = Color(0xFF2563EB), textColor = Color.White,
                            enabled = !isActing, modifier = Modifier.weight(1f)
                        ) { vm.setStatus(true) }
                        ActionBtn(
                            text = "停止", bgColor = Color.White,
                            borderColor = Color(0xFFEF4444), textColor = Color(0xFFEF4444),
                            enabled = !isActing, modifier = Modifier.weight(1f)
                        ) { vm.setStatus(false) }
                        ActionBtn(
                            text = "重载", bgColor = Color.White,
                            borderColor = Color(0xFFE4E4E7), textColor = Color(0xFF18181B),
                            enabled = !isActing, modifier = Modifier.weight(1f)
                        ) { vm.load() }
                    }
                }
            }

            // 管理菜单
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "网站管理",
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
                            val items = listOf(
                                "config" to "配置文件",
                                "rewrite" to "伪静态",
                                "ssl" to "SSL 证书",
                                "directory" to "网站目录",
                                "access_log" to "访问日志",
                                "error_log" to "错误日志",
                                "delete" to "删除网站"
                            )
                            items.forEachIndexed { idx, (key, label) ->
                                MenuItem(
                                    icon = { MenuSvgIcon(websiteFeatureIcon(key)) },
                                    label = label,
                                    onClick = {
                                        onMenuClick(key, website, webserver)
                                    }
                                )
                                if (idx < items.lastIndex) {
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
private fun DetailRow(label: String, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = label, fontSize = 13.sp, color = Color(0xFF71717A), modifier = Modifier.width(40.dp))
        Text(text = value, fontSize = 13.sp, color = Color(0xFF18181B))
    }
}

@Composable
private fun ActionBtn(
    text: String,
    bgColor: Color,
    textColor: Color,
    borderColor: Color = Color.Transparent,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .then(if (borderColor != Color.Transparent) Modifier.border(1.5.dp, borderColor, RoundedCornerShape(10.dp)) else Modifier)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = textColor)
    }
}
