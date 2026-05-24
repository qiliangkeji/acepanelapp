package com.acepanel.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.acepanel.app.viewmodel.AddDatabaseViewModel

@Composable
fun AddDatabaseScreen(
    panelId: String = "",
    onBackClick: () -> Unit = {},
    onSubmitClick: () -> Unit = {}
) {
    val vm = viewModel<AddDatabaseViewModel>()
    val servers by vm.servers.collectAsStateWithLifecycle()
    val selectedServerId by vm.selectedServerId.collectAsStateWithLifecycle()
    val dbName by vm.dbName.collectAsStateWithLifecycle()
    val createUser by vm.createUser.collectAsStateWithLifecycle()
    val username by vm.username.collectAsStateWithLifecycle()
    val password by vm.password.collectAsStateWithLifecycle()
    val host by vm.host.collectAsStateWithLifecycle()
    val comment by vm.comment.collectAsStateWithLifecycle()
    val isLoadingServers by vm.isLoadingServers.collectAsStateWithLifecycle()
    val submitStatus by vm.submitStatus.collectAsStateWithLifecycle()

    val isSubmitting = submitStatus is AddDatabaseViewModel.SubmitStatus.Loading
    val submitError = (submitStatus as? AddDatabaseViewModel.SubmitStatus.Error)?.message

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
            Text("添加数据库", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF18181B))
            Box(modifier = Modifier.size(36.dp))
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 数据库服务器
            item {
                DatabaseFormSection("数据库服务器") {
                    if (isLoadingServers) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else if (servers.isEmpty()) {
                        Text("未检测到可用的数据库服务器", fontSize = 13.sp, color = Color(0xFFA1A1AA))
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            servers.forEach { server ->
                                val isSelected = selectedServerId == server.id
                                Box(
                                    modifier = Modifier
                                        .height(36.dp)
                                        .background(
                                            if (isSelected) Color(0xFF2563EB) else Color(0xFFF3F4F6),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable { vm.selectedServerId.value = server.id }
                                        .padding(horizontal = 14.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = server.name.ifEmpty { "${server.type} (${server.host}:${server.port})" },
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else Color(0xFF71717A)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 数据库名
            item {
                DatabaseFormSection("数据库名") {
                    OutlinedTextField(
                        value = dbName,
                        onValueChange = { vm.dbName.value = it },
                        placeholder = { Text("my_database") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // 备注
            item {
                DatabaseFormSection("备注（可选）") {
                    OutlinedTextField(
                        value = comment,
                        onValueChange = { vm.comment.value = it },
                        placeholder = { Text("备注") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // 创建数据库用户
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "同时创建数据库用户",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF71717A),
                        letterSpacing = 1.sp
                    )
                    Switch(
                        checked = createUser,
                        onCheckedChange = { vm.createUser.value = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF2563EB),
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = Color(0xFFE4E4E7)
                        )
                    )
                }
            }

            if (createUser) {
                item {
                    DatabaseFormSection("用户名") {
                        OutlinedTextField(
                            value = username,
                            onValueChange = { vm.username.value = it },
                            placeholder = { Text("db_user") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
                item {
                    DatabaseFormSection("密码") {
                        OutlinedTextField(
                            value = password,
                            onValueChange = { vm.password.value = it },
                            placeholder = { Text("password") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
                item {
                    DatabaseFormSection("允许的主机") {
                        OutlinedTextField(
                            value = host,
                            onValueChange = { vm.host.value = it },
                            placeholder = { Text("localhost") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }

            if (submitError != null) {
                item {
                    Text(
                        text = submitError,
                        fontSize = 13.sp,
                        color = Color(0xFFEF4444)
                    )
                }
            }
        }

        // Submit
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSubmitting) Color(0xFFBFDBFE) else Color(0xFF2563EB))
                    .clickable(enabled = !isSubmitting) { vm.submit(onSubmitClick) },
                contentAlignment = Alignment.Center
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                } else {
                    Text("创建数据库", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun DatabaseFormSection(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF71717A),
            letterSpacing = 1.sp
        )
        content()
    }
}
