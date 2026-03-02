package org.example.project.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.example.project.data.AppDetailItem
import org.example.project.ui.components.StatusBarSpacer
import org.example.project.viewmodel.SoftwareStoreViewModel

@Composable
fun SoftwareStoreScreen(
    panelId: String = "",
    onBack: () -> Unit
) {
    val vm = viewModel<SoftwareStoreViewModel>()
    val categories by vm.categories.collectAsStateWithLifecycle()
    val selectedCategoryValue by vm.selectedCategoryValue.collectAsStateWithLifecycle()
    val searchQuery by vm.searchQuery.collectAsStateWithLifecycle()
    val isLoading by vm.isLoading.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    val operatingSlug by vm.operatingSlug.collectAsStateWithLifecycle()
    val operationError by vm.operationError.collectAsStateWithLifecycle()

    LaunchedEffect(panelId) {
        if (panelId.isNotEmpty()) vm.init(panelId)
    }

    // 本地过滤
    val displayApps = vm.filteredApps

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
                .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .border(1.5.dp, Color(0xFFE4E4E7), RoundedCornerShape(10.dp))
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "<", fontSize = 18.sp, color = Color(0xFF18181B))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "软件商店",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF18181B),
                    letterSpacing = (-0.5).sp
                )
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = Color(0xFF2563EB)
                    )
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { vm.searchQuery.value = it },
                placeholder = { Text("搜索软件名称或描述", color = Color(0xFFA1A1AA), fontSize = 14.sp) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
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

        // Categories
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, top = 0.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                val isSelected = selectedCategoryValue.isEmpty()
                Box(
                    modifier = Modifier
                        .height(32.dp)
                        .background(
                            if (isSelected) Color(0xFF2563EB) else Color(0xFFF3F4F6),
                            RoundedCornerShape(16.dp)
                        )
                        .clickable { vm.loadApps("") }
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "全部",
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (isSelected) Color.White else Color(0xFF71717A)
                    )
                }
            }
            items(categories) { cat ->
                val isSelected = cat.value == selectedCategoryValue
                Box(
                    modifier = Modifier
                        .height(32.dp)
                        .background(
                            if (isSelected) Color(0xFF2563EB) else Color(0xFFF3F4F6),
                            RoundedCornerShape(16.dp)
                        )
                        .clickable { vm.loadApps(cat.value) }
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = cat.label,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (isSelected) Color.White else Color(0xFF71717A)
                    )
                }
            }
        }

        if (operationError != null) {
            Text(
                text = "操作失败: $operationError",
                fontSize = 12.sp,
                color = Color(0xFFEF4444),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
            )
        }

        if (error != null) {
            Text(
                text = "加载失败: $error",
                fontSize = 12.sp,
                color = Color(0xFFEF4444),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
            )
        }

        // App List
        if (displayApps.isEmpty() && !isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = if (panelId.isEmpty()) "请先选择面板" else "暂无应用",
                    fontSize = 14.sp, color = Color(0xFFA1A1AA)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(displayApps) { app ->
                    AppDetailCard(
                        app = app,
                        isOperating = operatingSlug == app.slug,
                        onInstall = {
                            val channel = app.channels.firstOrNull()?.slug ?: "stable"
                            vm.installApp(app.slug, channel)
                        },
                        onUninstall = { vm.uninstallApp(app.slug) }
                    )
                }
            }
        }
    }
}

@Composable
fun AppDetailCard(
    app: AppDetailItem,
    isOperating: Boolean = false,
    onInstall: () -> Unit = {},
    onUninstall: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, Color(0xFFE4E4E7), RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon placeholder
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color(0xFFEFF6FF), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = app.name.take(1).uppercase(), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2563EB))
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = app.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF18181B)
                    )
                    val versionText = if (app.installed) {
                        "已安装 ${app.installed_version}".trim()
                    } else {
                        app.channels.firstOrNull()?.version ?: ""
                    }
                    if (versionText.isNotEmpty()) {
                        Text(text = versionText, fontSize = 13.sp, color = Color(0xFF71717A))
                    }
                }
            }

            if (isOperating) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = Color(0xFF2563EB)
                )
            } else {
                when {
                    app.update_exist -> {
                        Box(
                            modifier = Modifier
                                .height(28.dp)
                                .background(Color(0xFFFEF3C7), RoundedCornerShape(14.dp))
                                .clickable(onClick = onInstall)
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("更新", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFD97706))
                        }
                    }
                    app.installed -> {
                        Box(
                            modifier = Modifier
                                .height(28.dp)
                                .background(Color(0xFFECFDF5), RoundedCornerShape(14.dp))
                                .clickable(onClick = onUninstall)
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("已安装", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF059669))
                        }
                    }
                    else -> {
                        Box(
                            modifier = Modifier
                                .height(28.dp)
                                .background(Color(0xFF2563EB), RoundedCornerShape(14.dp))
                                .clickable(onClick = onInstall)
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("安装", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                        }
                    }
                }
            }
        }

        if (app.description.isNotEmpty()) {
            Text(text = app.description, fontSize = 13.sp, color = Color(0xFF71717A))
        }
    }
}
