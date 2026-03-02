package org.example.project.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
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
import org.example.project.ui.components.StatusBarSpacer
import org.example.project.viewmodel.DatabaseDetailViewModel

@Composable
fun DatabaseDetailScreen(
    panelId: String = "",
    databaseId: Long = 0,
    onBackClick: () -> Unit = {},
    onEditConfigClick: () -> Unit = {}
) {
    val vm = viewModel<DatabaseDetailViewModel>()
    val database by vm.database.collectAsStateWithLifecycle()
    val server by vm.server.collectAsStateWithLifecycle()
    val isLoading by vm.isLoading.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()

    LaunchedEffect(panelId, databaseId) {
        if (panelId.isNotEmpty() && databaseId > 0) vm.init(panelId, databaseId)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        StatusBarSpacer()

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
                text = database?.name ?: "数据库详情",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF18181B)
            )
            Box(modifier = Modifier.size(36.dp))
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            } else if (error != null) {
                Text(text = "加载失败: $error", fontSize = 13.sp, color = Color(0xFFEF4444))
            } else if (database != null) {
                val db = database!!
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.5.dp, Color(0xFFE4E4E7), RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "基本信息",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF71717A),
                            letterSpacing = 1.sp
                        )
                        DbDetailRow("名称", db.name)
                        DbDetailRow("类型", db.type.uppercase())
                        DbDetailRow("服务器", db.server.ifEmpty { db.server_id.toString() })
                        if (db.encoding.isNotEmpty()) DbDetailRow("字符集", db.encoding)
                        if (db.comment.isNotEmpty()) DbDetailRow("备注", db.comment)
                    }
                }

                if (server != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.5.dp, Color(0xFFE4E4E7), RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "服务器信息",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF71717A),
                                letterSpacing = 1.sp
                            )
                            DbDetailRow("名称", server!!.name)
                            DbDetailRow("地址", "${server!!.host}:${server!!.port}")
                            DbDetailRow("状态", if (server!!.status) "在线" else "离线")
                            if (server!!.remark.isNotEmpty()) DbDetailRow("备注", server!!.remark)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF2563EB))
                    .clickable(onClick = onEditConfigClick),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "服务器配置",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun DbDetailRow(label: String, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = label, fontSize = 13.sp, color = Color(0xFF71717A), modifier = Modifier.width(52.dp))
        Text(text = value, fontSize = 13.sp, color = Color(0xFF18181B))
    }
}
