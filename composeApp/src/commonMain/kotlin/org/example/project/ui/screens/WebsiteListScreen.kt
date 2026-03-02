package org.example.project.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import org.example.project.data.WebsiteApiItem
import org.example.project.ui.components.IconPlaceholder
import org.example.project.ui.components.StatusBarSpacer
import org.example.project.viewmodel.WebsiteListViewModel

@Composable
fun WebsiteListScreen(
    panelId: String = "",
    onBackClick: () -> Unit = {},
    onAddClick: () -> Unit = {},
    onWebsiteClick: (String) -> Unit = {}
) {
    val vm: WebsiteListViewModel = viewModel()
    val websites by vm.websites.collectAsStateWithLifecycle()
    val isLoading by vm.isLoading.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    val total by vm.total.collectAsStateWithLifecycle()
    val searchQuery by vm.searchQuery.collectAsStateWithLifecycle()

    LaunchedEffect(panelId) {
        if (panelId.isNotEmpty()) vm.init(panelId)
    }

    val filtered = vm.filteredWebsites()

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
                .padding(horizontal = 24.dp, vertical = 12.dp),
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
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.5.dp, Color(0xFFE4E4E7), RoundedCornerShape(10.dp))
                        .clickable(onClick = onBackClick),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "‹", fontSize = 20.sp, color = Color(0xFF18181B))
                }
                Text(
                    text = "网站管理",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF18181B)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = Color(0xFF2563EB)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF2563EB))
                            .clickable(onClick = onAddClick),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "+", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }

            // 搜索框
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { vm.searchQuery.value = it },
                placeholder = { Text("搜索网站域名或名称", color = Color(0xFFA1A1AA), fontSize = 14.sp) },
                modifier = Modifier.fillMaxWidth().height(44.dp),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF2563EB),
                    unfocusedBorderColor = Color(0xFFE4E4E7),
                    focusedTextColor = Color(0xFF18181B),
                    unfocusedTextColor = Color(0xFF18181B)
                )
            )
        }

        // 错误提示
        if (error != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFFEF2F2))
                    .padding(10.dp)
            ) {
                Text(text = "⚠ $error", fontSize = 12.sp, color = Color(0xFFEF4444))
            }
        }

        // 列表
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            item {
                Text(
                    text = "全部网站 (${if (searchQuery.isEmpty()) total else filtered.size})",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF71717A),
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            if (filtered.isEmpty() && !isLoading) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (searchQuery.isEmpty()) "暂无网站" else "未找到匹配的网站",
                            fontSize = 14.sp,
                            color = Color(0xFFA1A1AA)
                        )
                    }
                }
            }

            items(filtered, key = { it.id }) { website ->
                WebsiteCard(
                    website = website,
                    onClick = { onWebsiteClick(website.id.toString()) },
                    onToggleStatus = { vm.toggleStatus(website) },
                    onDelete = { vm.deleteWebsite(website.id) }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
fun WebsiteCard(
    website: WebsiteApiItem,
    onClick: () -> Unit = {},
    onToggleStatus: () -> Unit = {},
    onDelete: () -> Unit = {}
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("删除网站", fontWeight = FontWeight.SemiBold) },
            text = { Text("确定删除网站「${website.domains.firstOrNull()?.ifEmpty { website.name } ?: website.name}」？此操作不可恢复。") },
            confirmButton = {
                TextButton(onClick = { onDelete(); showDeleteConfirm = false }) {
                    Text("删除", color = Color(0xFFEF4444), fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("取消") }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.5.dp, Color(0xFFE4E4E7), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 域名（优先显示第一个域名，无则显示 name）
                Text(
                    text = website.domains.firstOrNull()?.ifEmpty { website.name } ?: website.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF18181B),
                    modifier = Modifier.weight(1f)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 状态徽章（可点击切换）
                    Box(
                        modifier = Modifier
                            .height(24.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (website.status) Color(0xFFECFDF5) else Color(0xFFFEF3C7)
                            )
                            .clickable(onClick = onToggleStatus)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (website.status) "运行中" else "已停止",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (website.status) Color(0xFF10B981) else Color(0xFFD97706)
                        )
                    }
                    // 删除按钮
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFFEF2F2))
                            .clickable { showDeleteConfirm = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("✕", fontSize = 10.sp, color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                    }
                }
            }

            // 域名列表 / 网站 name
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                if (website.domains.size > 1) {
                    Text(
                        text = website.domains.drop(1).joinToString(" · "),
                        fontSize = 12.sp,
                        color = Color(0xFF71717A)
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = website.type.uppercase(),
                        fontSize = 11.sp,
                        color = Color(0xFFA1A1AA)
                    )
                    if (website.php > 0) {
                        Text(
                            text = "PHP ${website.php}",
                            fontSize = 11.sp,
                            color = Color(0xFFA1A1AA)
                        )
                    }
                    if (website.ssl) {
                        Text(
                            text = "SSL",
                            fontSize = 11.sp,
                            color = Color(0xFF2563EB)
                        )
                    }
                }
            }
        }
    }
}

// 保持兼容性
@Composable
fun WebsiteStatusBadge(status: org.example.project.data.WebsiteStatus) {
    val (bgColor, textColor, text) = when (status) {
        org.example.project.data.WebsiteStatus.RUNNING -> Triple(Color(0xFFECFDF5), Color(0xFF10B981), "运行中")
        org.example.project.data.WebsiteStatus.STOPPED -> Triple(Color(0xFFFEF3C7), Color(0xFFD97706), "已停止")
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
