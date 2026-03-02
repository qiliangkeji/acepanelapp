package org.example.project.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.example.project.data.UserTokenItem
import org.example.project.ui.components.StatusBarSpacer
import org.example.project.viewmodel.TokenManagementViewModel

@Composable
fun TokenManagementScreen(
    panelId: String = "",
    userId: Long = 0L,
    onBack: () -> Unit = {}
) {
    val vm: TokenManagementViewModel = viewModel()
    val tokens by vm.tokens.collectAsStateWithLifecycle()
    val isLoading by vm.isLoading.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    val actionId by vm.actionId.collectAsStateWithLifecycle()
    val actionError by vm.actionError.collectAsStateWithLifecycle()
    val createResult by vm.createResult.collectAsStateWithLifecycle()
    val editToken by vm.showEditDialog.collectAsStateWithLifecycle()

    LaunchedEffect(panelId) {
        if (panelId.isNotEmpty()) vm.init(panelId, userId)
    }

    // 显示新建 Token 结果
    if (createResult != null) {
        Dialog(onDismissRequest = { vm.clearCreateResult() }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Token 已创建", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF18181B))
                Text("请立即复制并保存，关闭后不再显示：", fontSize = 13.sp, color = Color(0xFF71717A))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF3F4F6), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Text(createResult!!, fontSize = 12.sp, color = Color(0xFF18181B))
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF2563EB))
                        .clickable { vm.clearCreateResult() }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("已复制，关闭", fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Medium)
                }
            }
        }
    }

    var showCreate by remember { mutableStateOf(false) }
    if (showCreate) {
        CreateTokenDialog(
            onDismiss = { showCreate = false },
            onCreate = { days, ips ->
                val now = System.currentTimeMillis()
                val expMs = if (days > 0) now + days * 86400_000L else 0L
                vm.createToken(expMs, ips)
                showCreate = false
            }
        )
    }

    if (editToken != null) {
        EditTokenDialog(
            token = editToken!!,
            onDismiss = { vm.hideEdit() },
            onSave = { days, ips ->
                val now = System.currentTimeMillis()
                val expMs = if (days > 0) now + days * 86400_000L else 0L
                vm.updateToken(editToken!!.id, ips, expMs)
            }
        )
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
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Text("‹", fontSize = 20.sp, color = Color(0xFF18181B))
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Token 管理", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF18181B))
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color(0xFF2563EB))
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
                        .border(1.5.dp, Color(0xFFE4E4E7), RoundedCornerShape(10.dp))
                        .clickable { vm.load() },
                    contentAlignment = Alignment.Center
                ) { Text("↻", fontSize = 16.sp, color = Color(0xFF18181B)) }
                Box(
                    modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF2563EB))
                        .clickable { showCreate = true },
                    contentAlignment = Alignment.Center
                ) { Text("+", fontSize = 20.sp, color = Color.White, fontWeight = FontWeight.Bold) }
            }
        }

        if (error != null) {
            Text("加载失败: $error", fontSize = 12.sp, color = Color(0xFFEF4444),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp))
        }
        if (actionError != null) {
            Text(actionError!!, fontSize = 12.sp, color = Color(0xFFEF4444),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp))
        }

        if (tokens.isEmpty() && !isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(if (panelId.isEmpty()) "请先选择面板" else "暂无 Token", fontSize = 14.sp, color = Color(0xFFA1A1AA))
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                items(tokens) { token ->
                    TokenRow(
                        token = token,
                        isActing = actionId == token.id,
                        onEdit = { vm.showEdit(token) },
                        onDelete = { vm.deleteToken(token.id) }
                    )
                    HorizontalDivider(thickness = 0.5.dp, color = Color(0xFFF0F0F0))
                }
            }
        }
    }
}

@Composable
private fun TokenRow(token: UserTokenItem, isActing: Boolean, onEdit: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "ID: ${token.id}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF18181B)
            )
            val expiry = if (token.expired_at > 0) {
                val ms = token.expired_at
                val s = ms / 1000
                val days = (s - System.currentTimeMillis() / 1000) / 86400
                if (days > 0) "还有 ${days} 天过期" else "已过期"
            } else "永不过期"
            Text(expiry, fontSize = 12.sp, color = Color(0xFF71717A))
            if (token.ips.isNotEmpty()) {
                Text("IP白名单: ${token.ips.joinToString(", ")}", fontSize = 11.sp, color = Color(0xFFA1A1AA))
            }
        }

        if (isActing) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color(0xFF2563EB))
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .height(28.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .border(1.dp, Color(0xFF2563EB), RoundedCornerShape(6.dp))
                        .clickable(onClick = onEdit)
                        .padding(horizontal = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("编辑", fontSize = 11.sp, color = Color(0xFF2563EB), fontWeight = FontWeight.Medium)
                }
                Box(
                    modifier = Modifier
                        .height(28.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .border(1.dp, Color(0xFFEF4444), RoundedCornerShape(6.dp))
                        .clickable(onClick = onDelete)
                        .padding(horizontal = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("删除", fontSize = 11.sp, color = Color(0xFFEF4444), fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun CreateTokenDialog(onDismiss: () -> Unit, onCreate: (Long, List<String>) -> Unit) {
    var days by remember { mutableStateOf("365") }
    var ipsText by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("新建 Token", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF18181B))
            OutlinedTextField(
                value = days,
                onValueChange = { days = it },
                label = { Text("有效期（天）") },
                placeholder = { Text("365") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = ipsText,
                onValueChange = { ipsText = it },
                label = { Text("IP 白名单（逗号分隔，可空）") },
                placeholder = { Text("1.1.1.1, 2.2.2.0/24") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Text("留空 IP 白名单表示不限制来源 IP", fontSize = 12.sp, color = Color(0xFF71717A))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("取消", color = Color(0xFF71717A)) }
                Spacer(Modifier.width(8.dp))
                val daysLong = days.trim().toLongOrNull() ?: -1L
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (daysLong >= 0) Color(0xFF2563EB) else Color(0xFF71717A))
                        .clickable(enabled = daysLong >= 0) { onCreate(daysLong, parseIps(ipsText)) }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text("创建", fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun EditTokenDialog(
    token: UserTokenItem,
    onDismiss: () -> Unit,
    onSave: (Long, List<String>) -> Unit
) {
    val remainDays = remember(token.id, token.expired_at) {
        if (token.expired_at <= 0L) 0L
        else ((token.expired_at - System.currentTimeMillis()) / 86400_000L).coerceAtLeast(0L)
    }
    var days by remember(token.id) { mutableStateOf(remainDays.toString()) }
    var ipsText by remember(token.id) { mutableStateOf(token.ips.joinToString(", ")) }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("编辑 Token", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF18181B))
            OutlinedTextField(
                value = days,
                onValueChange = { days = it },
                label = { Text("有效期（天，0=不过期）") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = ipsText,
                onValueChange = { ipsText = it },
                label = { Text("IP 白名单（逗号分隔，可空）") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("取消", color = Color(0xFF71717A)) }
                Spacer(Modifier.width(8.dp))
                val daysLong = days.trim().toLongOrNull() ?: -1L
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (daysLong >= 0) Color(0xFF2563EB) else Color(0xFF71717A))
                        .clickable(enabled = daysLong >= 0) { onSave(daysLong, parseIps(ipsText)) }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text("保存", fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

private fun parseIps(text: String): List<String> {
    return text.split(",")
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .distinct()
}
