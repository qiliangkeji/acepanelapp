package org.example.project.ui.screens

import androidx.compose.foundation.background
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
import kotlinx.coroutines.launch
import org.example.project.data.PanelRepository
import org.example.project.network.PanelApiService
import org.example.project.ui.components.BackButton
import org.example.project.ui.components.StatusBarSpacer

@Composable
fun DeleteWebsiteConfirmScreen(
    panelId: String = "",
    websiteId: String,
    onBackClick: () -> Unit = {},
    onConfirmDelete: () -> Unit = {}
) {
    val websiteIdLong = websiteId.toLongOrNull() ?: 0L
    var deletePath by remember { mutableStateOf(false) }
    var deleteDb by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

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
            BackButton(onClick = onBackClick)
            Text(
                text = "删除网站",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF18181B)
            )
            Spacer(modifier = Modifier.size(36.dp))
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "即将删除网站 #$websiteId，此操作不可撤销。",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF18181B)
                )

                // 删除选项
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF9FAFB))
                        .clickable { deletePath = !deletePath }
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("同时删除网站目录", fontSize = 14.sp, color = Color(0xFF18181B))
                        Text("删除 .../sites/{name} 下的所有文件", fontSize = 12.sp, color = Color(0xFF71717A))
                    }
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (deletePath) Color(0xFF2563EB) else Color(0xFFE4E4E7)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (deletePath) Text("✓", fontSize = 12.sp, color = Color.White)
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF9FAFB))
                        .clickable { deleteDb = !deleteDb }
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("同时删除关联数据库", fontSize = 14.sp, color = Color(0xFF18181B))
                        Text("删除与网站同名的本地数据库和用户", fontSize = 12.sp, color = Color(0xFF71717A))
                    }
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (deleteDb) Color(0xFF2563EB) else Color(0xFFE4E4E7)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (deleteDb) Text("✓", fontSize = 12.sp, color = Color.White)
                    }
                }

                if (errorMsg != null) {
                    Text(
                        text = "删除失败：$errorMsg",
                        fontSize = 13.sp,
                        color = Color(0xFFEF4444)
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF3F4F6))
                        .clickable(onClick = onBackClick),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "取消",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF18181B)
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isDeleting) Color(0xFFEF4444).copy(alpha = 0.5f) else Color(0xFFEF4444))
                        .clickable(enabled = !isDeleting) {
                            scope.launch {
                                isDeleting = true
                                errorMsg = null
                                val cfg = PanelRepository.getPanel(panelId)
                                if (cfg == null) {
                                    errorMsg = "未找到面板配置"
                                    isDeleting = false
                                    return@launch
                                }
                                val service = PanelApiService(cfg)
                                service.deleteWebsite(websiteIdLong, deletePath, deleteDb)
                                    .onSuccess {
                                        service.close()
                                        onConfirmDelete()
                                    }
                                    .onFailure { e ->
                                        service.close()
                                        errorMsg = e.message
                                        isDeleting = false
                                    }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isDeleting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                    } else {
                        Text(
                            text = "确认删除",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
