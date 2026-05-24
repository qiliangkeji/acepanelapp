package com.acepanel.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.acepanel.app.data.DatabaseApiItem
import com.acepanel.app.data.DatabaseServerApiItem
import com.acepanel.app.ui.components.SegmentedTabs
import com.acepanel.app.ui.components.StatusBarSpacer
import com.acepanel.app.ui.components.rememberTabBackStack
import com.acepanel.app.viewmodel.DatabaseListViewModel

@Composable
fun DatabaseListScreen(
    panelId: String = "",
    onBackClick: () -> Unit = {},
    onAddClick: () -> Unit = {},
    onDatabaseClick: (DatabaseApiItem) -> Unit = {}
) {
    val vm: DatabaseListViewModel = viewModel()
    val databases by vm.databases.collectAsStateWithLifecycle()
    val servers by vm.servers.collectAsStateWithLifecycle()
    val isLoading by vm.isLoading.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    val tabStack = rememberTabBackStack()
    val selectedTab = tabStack.current

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
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.5.dp, Color(0xFFE4E4E7), RoundedCornerShape(10.dp))
                    .clickable(onClick = onBackClick),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "‹", fontSize = 20.sp, color = Color(0xFF18181B))
            }
            Text(
                text = "数据库",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF18181B)
            )
            if (selectedTab == 0) {
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
            } else {
                Spacer(modifier = Modifier.size(36.dp))
            }
        }

        SegmentedTabs(
            labels = listOf("数据库", "用户", "服务器", "Redis键"),
            selectedIndex = selectedTab,
            onSelected = { tabStack.select(it) },
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
        )

        when (selectedTab) {
            0 -> DatabaseListContent(
                databases = databases,
                servers = servers,
                isLoading = isLoading,
                error = error,
                onDatabaseClick = onDatabaseClick,
                onDeleteDatabase = { serverId, name -> vm.deleteDatabase(serverId, name) }
            )
            1 -> DatabaseUserScreen(
                panelId = panelId,
                showStatusBarSpacer = false,
                showBackButton = false
            )
            2 -> DatabaseServerScreen(
                panelId = panelId,
                showStatusBarSpacer = false,
                showBackButton = false
            )
            3 -> RedisKeyScreen(
                panelId = panelId,
                showStatusBarSpacer = false,
                showBackButton = false
            )
        }
    }
}

@Composable
private fun DatabaseListContent(
    databases: List<DatabaseApiItem>,
    servers: List<DatabaseServerApiItem>,
    isLoading: Boolean,
    error: String?,
    onDatabaseClick: (DatabaseApiItem) -> Unit,
    onDeleteDatabase: (Long, String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // 错误提示
        if (error != null) {
            item {
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
        }

        // 数据库服务器概览卡片
        if (servers.isNotEmpty()) {
            item {
                Text(
                    text = "数据库服务",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF71717A),
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )
            }
            items(servers, key = { "srv_${it.id}" }) { server ->
                DatabaseServerCard(server = server)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // 数据库列表
        item {
            Text(
                text = "数据库列表 (${databases.size})",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF71717A),
                letterSpacing = 1.sp,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )
        }

        if (databases.isEmpty() && !isLoading) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "暂无数据库", fontSize = 14.sp, color = Color(0xFFA1A1AA))
                }
            }
        }

        items(databases, key = { "${it.server_id}:${it.name}" }) { db ->
            DatabaseApiCard(
                database = db,
                onClick = { onDatabaseClick(db) },
                onDelete = { onDeleteDatabase(db.server_id, db.name) }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
fun DatabaseServerCard(server: DatabaseServerApiItem) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(1.5.dp, Color(0xFFE4E4E7), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = server.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF18181B)
                )
                Text(
                    text = "${server.type.uppercase()} · ${server.host}:${server.port}",
                    fontSize = 12.sp,
                    color = Color(0xFF71717A)
                )
            }
            Box(
                modifier = Modifier
                    .height(24.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (server.status) Color(0xFFECFDF5) else Color(0xFFFEF3C7)
                    )
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (server.status) "运行中" else "停止",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (server.status) Color(0xFF10B981) else Color(0xFFD97706)
                )
            }
        }
    }
}

@Composable
fun DatabaseApiCard(database: DatabaseApiItem, onClick: () -> Unit = {}, onDelete: () -> Unit = {}) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("删除数据库", fontWeight = FontWeight.SemiBold) },
            text = { Text("确定删除数据库「${database.name}」？此操作不可恢复。") },
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
            .padding(horizontal = 24.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(1.5.dp, Color(0xFFE4E4E7), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = database.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF18181B),
                    modifier = Modifier.weight(1f)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .height(22.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFF4F4F5))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = database.type.uppercase(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF71717A)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFFEF2F2))
                            .clickable { showDeleteConfirm = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("✕", fontSize = 10.sp, color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (database.server.isNotEmpty()) {
                    Text(text = database.server, fontSize = 12.sp, color = Color(0xFFA1A1AA))
                }
                if (database.encoding.isNotEmpty()) {
                    Text(text = "·", fontSize = 12.sp, color = Color(0xFFE4E4E7))
                    Text(text = database.encoding, fontSize = 12.sp, color = Color(0xFFA1A1AA))
                }
            }
            if (database.comment.isNotEmpty()) {
                Text(text = database.comment, fontSize = 12.sp, color = Color(0xFF71717A))
            }
        }
    }
}

// 保持旧接口兼容性
@Composable
fun DatabaseCard(database: com.acepanel.app.data.Database, onClick: () -> Unit = {}) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
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
                Text(text = database.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF18181B))
                Text(text = database.size, fontSize = 13.sp, color = Color(0xFF71717A))
            }
            Text(text = "字符集: ${database.charset}", fontSize = 13.sp, color = Color(0xFFA1A1AA))
        }
    }
}
