package org.example.project.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.example.project.data.RedisKVItem
import org.example.project.ui.components.StatusBarSpacer
import org.example.project.viewmodel.RedisKeyViewModel

@Composable
fun RedisKeyScreen(
    panelId: String,
    onBack: () -> Unit = {}
) {
    val vm: RedisKeyViewModel = viewModel()
    val redisServers by vm.redisServers.collectAsStateWithLifecycle()
    val selectedServerId by vm.selectedServerId.collectAsStateWithLifecycle()
    val dbCount by vm.dbCount.collectAsStateWithLifecycle()
    val selectedDb by vm.selectedDb.collectAsStateWithLifecycle()
    val keys by vm.keys.collectAsStateWithLifecycle()
    val searchText by vm.searchText.collectAsStateWithLifecycle()
    val isLoading by vm.isLoading.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    val actionKey by vm.actionKey.collectAsStateWithLifecycle()
    val actionError by vm.actionError.collectAsStateWithLifecycle()
    val detailKey by vm.detailKey.collectAsStateWithLifecycle()
    val isDetailLoading by vm.isDetailLoading.collectAsStateWithLifecycle()
    val showCreateDialog by vm.showCreateDialog.collectAsStateWithLifecycle()

    LaunchedEffect(panelId) { vm.init(panelId) }

    var showClearConfirm by remember { mutableStateOf(false) }
    var showServerDropdown by remember { mutableStateOf(false) }

    if (showCreateDialog) {
        CreateRedisKeyDialog(
            actionError = actionError,
            onDismiss = { vm.hideCreate() },
            onCreate = { key, value, type, ttl -> vm.createKey(key, value, type, ttl) }
        )
    }

    Column(
        modifier = Modifier.fillMaxSize().background(Color.White)
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
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Text("‹", fontSize = 20.sp, color = Color(0xFF18181B))
            }
            Text(
                "Redis 键管理",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF18181B)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF2563EB))
                        .clickable { vm.showCreate() },
                    contentAlignment = Alignment.Center
                ) { Text("+", fontSize = 20.sp, color = Color.White, fontWeight = FontWeight.Bold) }
                Box(
                    modifier = Modifier
                        .height(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFFEF2F2))
                        .clickable { showClearConfirm = true }
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("清空DB", fontSize = 13.sp, color = Color(0xFFEF4444), fontWeight = FontWeight.Medium)
                }
            }
        }

        // 服务器选择
        if (redisServers.size > 1) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 4.dp)
            ) {
                val selectedServer = redisServers.find { it.id == selectedServerId }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.5.dp, Color(0xFFE4E4E7), RoundedCornerShape(10.dp))
                        .clickable { showServerDropdown = true }
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            selectedServer?.name ?: "选择 Redis 服务器",
                            fontSize = 14.sp,
                            color = Color(0xFF18181B)
                        )
                        Text("▾", fontSize = 14.sp, color = Color(0xFF71717A))
                    }
                }
                DropdownMenu(
                    expanded = showServerDropdown,
                    onDismissRequest = { showServerDropdown = false }
                ) {
                    redisServers.forEach { server ->
                        DropdownMenuItem(
                            text = { Text(server.name) },
                            onClick = {
                                vm.selectServer(server.id)
                                showServerDropdown = false
                            }
                        )
                    }
                }
            }
        }

        // DB 选择 Tabs
        if (dbCount > 1) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(dbCount) { db ->
                    val selected = db == selectedDb
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selected) Color(0xFF2563EB) else Color(0xFFF4F4F5))
                            .clickable { vm.selectDb(db) }
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "DB $db",
                            fontSize = 13.sp,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (selected) Color.White else Color(0xFF71717A)
                        )
                    }
                }
            }
        }

        // 搜索栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchText,
                onValueChange = { vm.setSearch(it) },
                modifier = Modifier.weight(1f),
                placeholder = { Text("搜索键名", fontSize = 14.sp, color = Color(0xFFA1A1AA)) },
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
                    .clickable { vm.search() }
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("搜索", fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Medium)
            }
        }

        // 错误提示
        if (error != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFFEF2F2))
                    .padding(12.dp)
            ) {
                Text("⚠ $error", fontSize = 13.sp, color = Color(0xFFEF4444))
            }
        }
        if (actionError != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFFEF2F2))
                    .padding(12.dp)
            ) {
                Text("⚠ $actionError", fontSize = 13.sp, color = Color(0xFFEF4444))
            }
        }

        // 键列表
        if (isLoading) {
            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF2563EB))
            }
        } else if (keys.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                Text(
                    if (selectedServerId == null) "请先选择 Redis 服务器" else "暂无键数据",
                    fontSize = 14.sp,
                    color = Color(0xFFA1A1AA)
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.5.dp, Color(0xFFE4E4E7), RoundedCornerShape(16.dp))
            ) {
                Column {
                    keys.forEachIndexed { index, item ->
                        RedisKeyRow(
                            item = item,
                            isDeleting = actionKey == item.key,
                            onDelete = { vm.deleteKey(item.key) },
                            onClick = { vm.showKeyDetail(item) }
                        )
                        if (index < keys.lastIndex) {
                            HorizontalDivider(thickness = 1.dp, color = Color(0xFFE4E4E7))
                        }
                    }
                }
            }
        }
    }

    // 键详情弹窗
    if (detailKey != null) {
        AlertDialog(
            onDismissRequest = { vm.hideKeyDetail() },
            title = {
                Text(
                    detailKey!!.key,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF18181B)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (isDetailLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp).align(Alignment.CenterHorizontally),
                            color = Color(0xFF2563EB)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        RedisTag("类型: ${detailKey!!.type.uppercase()}", Color(0xFF7C3AED), Color(0xFFF5F3FF))
                        if (detailKey!!.ttl >= 0) {
                            RedisTag("TTL: ${detailKey!!.ttl}s", Color(0xFFF59E0B), Color(0xFFFFFBEB))
                        } else {
                            RedisTag("永不过期", Color(0xFF10B981), Color(0xFFECFDF5))
                        }
                    }
                    if (detailKey!!.value.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFF4F4F5))
                                .padding(12.dp)
                        ) {
                            Text(
                                detailKey!!.value,
                                fontSize = 13.sp,
                                color = Color(0xFF18181B)
                            )
                        }
                    }
                    if (detailKey!!.length > 0) {
                        Text(
                            "长度: ${detailKey!!.length}  大小: ${detailKey!!.size} bytes",
                            fontSize = 12.sp,
                            color = Color(0xFF71717A)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { vm.hideKeyDetail() }) {
                    Text("关闭", color = Color(0xFF2563EB))
                }
            }
        )
    }

    // 清空 DB 确认弹窗
    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("清空 DB $selectedDb", fontWeight = FontWeight.SemiBold) },
            text = { Text("此操作将删除当前数据库中的所有键，不可恢复。确认继续？") },
            confirmButton = {
                TextButton(onClick = {
                    vm.clearDb()
                    showClearConfirm = false
                }) {
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
private fun RedisKeyRow(
    item: RedisKVItem,
    isDeleting: Boolean,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                item.key,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF18181B),
                maxLines = 1
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RedisTag(item.type.uppercase(), Color(0xFF7C3AED), Color(0xFFF5F3FF))
                val ttlText = if (item.ttl < 0) "永久" else "${item.ttl}s"
                Text(ttlText, fontSize = 11.sp, color = Color(0xFFA1A1AA))
            }
        }
        if (isDeleting) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color(0xFFEF4444))
        } else {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = onDelete)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text("删除", fontSize = 12.sp, color = Color(0xFFEF4444))
            }
        }
    }
}

@Composable
private fun CreateRedisKeyDialog(
    actionError: String?,
    onDismiss: () -> Unit,
    onCreate: (String, String, String, Long) -> Unit
) {
    var key by remember { mutableStateOf("") }
    var value by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("string") }
    var ttl by remember { mutableStateOf("-1") }
    val typeOptions = listOf("string", "list", "hash", "set", "zset")

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                .background(Color.White).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("新建 Redis 键", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF18181B))

            OutlinedTextField(value = key, onValueChange = { key = it },
                label = { Text("键名") }, modifier = Modifier.fillMaxWidth(), singleLine = true)

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("类型", fontSize = 13.sp, color = Color(0xFF71717A))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    typeOptions.forEach { t ->
                        Box(
                            modifier = Modifier.height(28.dp)
                                .background(if (type == t) Color(0xFF2563EB) else Color(0xFFF3F4F6), RoundedCornerShape(6.dp))
                                .clickable { type = t }.padding(horizontal = 10.dp),
                            contentAlignment = Alignment.Center
                        ) { Text(t, fontSize = 12.sp, color = if (type == t) Color.White else Color(0xFF71717A)) }
                    }
                }
            }

            OutlinedTextField(value = value, onValueChange = { value = it },
                label = { Text("值") }, modifier = Modifier.fillMaxWidth().height(72.dp), maxLines = 3)

            OutlinedTextField(value = ttl, onValueChange = { ttl = it },
                label = { Text("TTL（秒，-1 永不过期）") }, modifier = Modifier.fillMaxWidth(), singleLine = true)

            if (actionError != null) {
                Text(actionError, fontSize = 12.sp, color = Color(0xFFEF4444))
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("取消", color = Color(0xFF71717A)) }
                Spacer(Modifier.width(8.dp))
                val ttlLong = ttl.toLongOrNull() ?: -1L
                val canSubmit = key.isNotBlank()
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(8.dp))
                        .background(if (canSubmit) Color(0xFF2563EB) else Color(0xFF71717A))
                        .clickable(enabled = canSubmit) { onCreate(key.trim(), value, type, ttlLong) }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) { Text("创建", fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Medium) }
            }
        }
    }
}

@Composable
private fun RedisTag(text: String, textColor: Color, bgColor: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bgColor)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(text, fontSize = 11.sp, color = textColor, fontWeight = FontWeight.Medium)
    }
}
