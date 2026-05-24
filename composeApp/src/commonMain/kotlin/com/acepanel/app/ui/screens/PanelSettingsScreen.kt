package com.acepanel.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.acepanel.app.ui.components.AnimatedAppDialog
import com.acepanel.app.ui.components.SegmentedTabs
import com.acepanel.app.ui.components.StatusBarSpacer
import com.acepanel.app.ui.components.rememberTabBackStack
import com.acepanel.app.viewmodel.PanelSettingsViewModel

@Composable
fun PanelSettingsScreen(
    panelId: String = "",
    onBackClick: () -> Unit = {}
) {
    val vm = viewModel<PanelSettingsViewModel>()
    val setting by vm.setting.collectAsStateWithLifecycle()
    val isLoading by vm.isLoading.collectAsStateWithLifecycle()
    val isSaving by vm.isSaving.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    val saveError by vm.saveError.collectAsStateWithLifecycle()
    val restartRequired by vm.restartRequired.collectAsStateWithLifecycle()
    val memo by vm.memo.collectAsStateWithLifecycle()
    val isMemoLoading by vm.isMemoLoading.collectAsStateWithLifecycle()

    var showPortDialog by remember { mutableStateOf(false) }
    var showLifetimeDialog by remember { mutableStateOf(false) }
    var showEntranceDialog by remember { mutableStateOf(false) }
    var showMemoDialog by remember { mutableStateOf(false) }
    var showChannelDialog by remember { mutableStateOf(false) }
    val sectionStack = rememberTabBackStack()
    val selectedSection = sectionStack.current

    LaunchedEffect(panelId) {
        if (panelId.isNotEmpty()) vm.init(panelId)
    }

    // 弹窗
    if (showPortDialog) {
        SingleFieldDialog(
            title = "面板端口",
            label = "端口号（1-65535）",
            initialValue = setting?.port?.toString() ?: "",
            onDismiss = { showPortDialog = false },
            onConfirm = { v -> vm.updatePort(v.toIntOrNull() ?: 8888); showPortDialog = false }
        )
    }
    if (showLifetimeDialog) {
        SingleFieldDialog(
            title = "超时时间",
            label = "会话超时（分钟，10-43200）",
            initialValue = setting?.lifetime?.toString() ?: "",
            onDismiss = { showLifetimeDialog = false },
            onConfirm = { v -> vm.updateLifetime(v.toIntOrNull() ?: 60); showLifetimeDialog = false }
        )
    }
    if (showEntranceDialog) {
        EntranceDialog(
            currentEntrance = setting?.entrance ?: "",
            currentMode = setting?.entrance_error ?: "nginx",
            isSaving = isSaving,
            onDismiss = { showEntranceDialog = false },
            onSave = { entrance, mode ->
                vm.updateEntrance(entrance)
                if (mode != setting?.entrance_error) vm.updateEntranceError(mode)
                showEntranceDialog = false
            }
        )
    }
    if (showMemoDialog) {
        MemoDialog(
            initialContent = memo,
            onDismiss = { showMemoDialog = false },
            onSave = { content -> vm.saveMemo(content); showMemoDialog = false }
        )
    }
    if (showChannelDialog) {
        ChannelDialog(
            current = setting?.channel ?: "stable",
            onDismiss = { showChannelDialog = false },
            onSelect = { ch -> vm.updateChannel(ch); showChannelDialog = false }
        )
    }
    if (restartRequired) {
        AnimatedAppDialog(onDismissRequest = { vm.dismissRestart() }) {
            Column(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                    .background(Color.White).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("面板即将重启", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF18181B))
                Text("设置已保存，面板将重启以应用新配置。重启后请重新连接。", fontSize = 14.sp, color = Color(0xFF71717A))
                TextButton(onClick = { vm.dismissRestart() }, modifier = Modifier.align(Alignment.End)) {
                    Text("知道了", color = Color(0xFF2563EB))
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
        StatusBarSpacer()
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
        Box(
            modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
                .border(1.5.dp, Color(0xFFE4E4E7), RoundedCornerShape(10.dp))
                    .clickable(onClick = onBackClick),
                contentAlignment = Alignment.Center
            ) { Text(text = "‹", fontSize = 20.sp, color = Color(0xFF18181B)) }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("面板设置", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF18181B))
                if (isLoading || isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color(0xFF2563EB))
                }
            }

            Box(
                modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
                    .border(1.5.dp, Color(0xFFE4E4E7), RoundedCornerShape(10.dp))
                    .clickable { vm.load(); vm.loadMemo() },
                contentAlignment = Alignment.Center
            ) { Text("↻", fontSize = 16.sp, color = Color(0xFF18181B)) }
        }

        SegmentedTabs(
            labels = listOf("设置", "Token 管理"),
            selectedIndex = selectedSection,
            onSelected = { sectionStack.select(it) },
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
        )

        if (selectedSection == 1) {
            TokenManagementScreen(
                panelId = panelId,
                showStatusBarSpacer = false,
                showBackButton = false
            )
            return@Column
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 信息卡
            if (setting != null) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFFEFF6FF)).padding(16.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = setting!!.name.ifEmpty { "面板" },
                                fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF18181B)
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                SettingInfoItem("端口", setting!!.port.toString())
                                SettingInfoItem("入口", setting!!.entrance.ifEmpty { "（无）" })
                                SettingInfoItem("超时", "${setting!!.lifetime}min")
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                SettingInfoItem("渠道", setting!!.channel)
                                SettingInfoItem("自动更新", if (setting!!.auto_update) "开" else "关")
                                SettingInfoItem("验证码", if (setting!!.login_captcha) "开" else "关")
                            }
                        }
                    }
                }
            }

            if (error != null) {
                item { Text("加载失败: $error", fontSize = 13.sp, color = Color(0xFFEF4444)) }
            }
            if (saveError != null) {
                item { Text("保存失败: $saveError", fontSize = 13.sp, color = Color(0xFFEF4444)) }
            }

            // 便签
            item {
                SectionHeader("便签")
                Box(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                        .border(1.5.dp, Color(0xFFE4E4E7), RoundedCornerShape(16.dp))
                        .clickable { showMemoDialog = true }.padding(16.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("便签内容", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF18181B))
                            if (isMemoLoading) {
                                Text("加载中...", fontSize = 12.sp, color = Color(0xFFA1A1AA))
                            } else if (memo.isEmpty()) {
                                Text("点击添加便签内容", fontSize = 12.sp, color = Color(0xFFA1A1AA))
                            } else {
                                Text(memo.take(80), fontSize = 12.sp, color = Color(0xFF71717A), maxLines = 2)
                            }
                        }
                        Text("›", fontSize = 18.sp, color = Color(0xFFA1A1AA))
                    }
                }
            }

            // 基本设置
            item {
                SectionHeader("基本设置")
                Box(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                        .border(1.5.dp, Color(0xFFE4E4E7), RoundedCornerShape(16.dp))
                ) {
                    Column {
                        EditableSettingRow("面板端口", setting?.port?.toString() ?: "—") { showPortDialog = true }
                        HorizontalDivider(thickness = 1.dp, color = Color(0xFFE4E4E7))
                        EditableSettingRow("安全入口", setting?.entrance?.ifEmpty { "（未设置）" } ?: "—") { showEntranceDialog = true }
                        HorizontalDivider(thickness = 1.dp, color = Color(0xFFE4E4E7))
                        EditableSettingRow("超时时间", setting?.let { "${it.lifetime} 分钟" } ?: "—") { showLifetimeDialog = true }
                        HorizontalDivider(thickness = 1.dp, color = Color(0xFFE4E4E7))
                        ToggleSettingRow("登录验证码", setting?.login_captcha ?: false, enabled = setting != null) {
                            vm.updateLoginCaptcha(it)
                        }
                    }
                }
            }

            // 高级设置
            item {
                SectionHeader("高级设置")
                Box(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                        .border(1.5.dp, Color(0xFFE4E4E7), RoundedCornerShape(16.dp))
                ) {
                    Column {
                        ToggleSettingRow("自动更新", setting?.auto_update ?: false, enabled = setting != null) {
                            vm.updateAutoUpdate(it)
                        }
                        HorizontalDivider(thickness = 1.dp, color = Color(0xFFE4E4E7))
                        EditableSettingRow("更新渠道", setting?.channel ?: "—") { showChannelDialog = true }
                        HorizontalDivider(thickness = 1.dp, color = Color(0xFFE4E4E7))
                        ReadOnlySettingRow("调试模式", "（只读，无法通过 API 切换）")
                    }
                }
            }

            // 账户与安全
            item {
                SectionHeader("账户与安全")
                Box(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                        .border(1.5.dp, Color(0xFFE4E4E7), RoundedCornerShape(16.dp))
                ) {
                    Column {
                        EditableSettingRow("API 密钥", "请切换到 Token 管理") { sectionStack.select(1) }
                    }
                }
            }

            // 其他
            item {
                SectionHeader("其他")
                Box(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                        .border(1.5.dp, Color(0xFFE4E4E7), RoundedCornerShape(16.dp))
                ) {
                    Column {
                        ReadOnlySettingRow("关于面板", "")
                        HorizontalDivider(thickness = 1.dp, color = Color(0xFFE4E4E7))
                        ReadOnlySettingRow("使用文档", "")
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF71717A), letterSpacing = 1.sp,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun EditableSettingRow(label: String, value: String?, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 14.sp, color = Color(0xFF18181B))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (!value.isNullOrEmpty()) Text(value, fontSize = 13.sp, color = Color(0xFF71717A))
            Text("›", fontSize = 18.sp, color = Color(0xFFA1A1AA))
        }
    }
}

@Composable
private fun ToggleSettingRow(label: String, checked: Boolean, enabled: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 14.sp, color = Color(0xFF18181B))
        Switch(
            checked = checked, onCheckedChange = onToggle, enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF2563EB),
                uncheckedThumbColor = Color.White, uncheckedTrackColor = Color(0xFFE4E4E7)
            )
        )
    }
}

@Composable
private fun ReadOnlySettingRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 14.sp, color = Color(0xFF18181B))
        if (value.isNotEmpty()) Text(value, fontSize = 13.sp, color = Color(0xFF71717A))
        else Text("›", fontSize = 18.sp, color = Color(0xFFA1A1AA))
    }
}

@Composable
private fun SettingInfoItem(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(text = label, fontSize = 11.sp, color = Color(0xFF71717A))
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFF18181B))
    }
}

@Composable
private fun SingleFieldDialog(
    title: String, label: String, initialValue: String,
    onDismiss: () -> Unit, onConfirm: (String) -> Unit
) {
    var value by remember { mutableStateOf(initialValue) }
    AnimatedAppDialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                .background(Color.White).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(title, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF18181B))
            OutlinedTextField(
                value = value, onValueChange = { value = it },
                label = { Text(label) }, modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("取消", color = Color(0xFF71717A)) }
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Color(0xFF2563EB))
                        .clickable(enabled = value.isNotBlank()) { onConfirm(value.trim()) }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) { Text("保存", fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Medium) }
            }
        }
    }
}

@Composable
private fun EntranceDialog(
    currentEntrance: String,
    currentMode: String,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var entrance by remember { mutableStateOf(currentEntrance) }
    var mode by remember { mutableStateOf(currentMode) }
    val modeOptions = listOf("nginx" to "Nginx 默认页", "418" to "418 状态码", "close" to "直接关闭")

    AnimatedAppDialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                .background(Color.White).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("安全入口", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF18181B))

            OutlinedTextField(
                value = entrance, onValueChange = { entrance = it },
                label = { Text("入口路径（留空=关闭）") },
                placeholder = { Text("/myentrance") },
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("入口未开启时的响应方式", fontSize = 12.sp, color = Color(0xFF71717A))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    modeOptions.forEach { (key, label) ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { mode = key }.padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier.size(18.dp).border(
                                    1.5.dp,
                                    if (mode == key) Color(0xFF2563EB) else Color(0xFFE4E4E7),
                                    RoundedCornerShape(9.dp)
                                ).background(
                                    if (mode == key) Color(0xFF2563EB) else Color.White,
                                    RoundedCornerShape(9.dp)
                                )
                            )
                            Text(label, fontSize = 13.sp, color = Color(0xFF18181B))
                        }
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("取消", color = Color(0xFF71717A)) }
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(8.dp))
                        .background(if (!isSaving) Color(0xFF2563EB) else Color(0xFF71717A))
                        .clickable(enabled = !isSaving) { onSave(entrance.trim(), mode) }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                    } else {
                        Text("保存", fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

@Composable
private fun ChannelDialog(
    current: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    AnimatedAppDialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                .background(Color.White).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("更新渠道", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF18181B))
            listOf("stable" to "稳定版（推荐）", "beta" to "测试版").forEach { (key, label) ->
                Box(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                        .background(if (current == key) Color(0xFFEFF6FF) else Color(0xFFF9FAFB))
                        .border(
                            1.5.dp,
                            if (current == key) Color(0xFF2563EB) else Color(0xFFE4E4E7),
                            RoundedCornerShape(10.dp)
                        )
                        .clickable { onSelect(key) }.padding(12.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(label, fontSize = 14.sp, color = Color(0xFF18181B))
                        if (current == key) Text("✓", fontSize = 14.sp, color = Color(0xFF2563EB), fontWeight = FontWeight.Bold)
                    }
                }
            }
            TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                Text("取消", color = Color(0xFF71717A))
            }
        }
    }
}

@Composable
private fun MemoDialog(
    initialContent: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var content by remember { mutableStateOf(initialContent) }
    AnimatedAppDialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                .background(Color.White).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("便签", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF18181B))
            OutlinedTextField(
                value = content, onValueChange = { content = it },
                label = { Text("便签内容") },
                placeholder = { Text("记录一些重要信息...") },
                modifier = Modifier.fillMaxWidth().height(160.dp),
                maxLines = 8
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("取消", color = Color(0xFF71717A)) }
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Color(0xFF2563EB))
                        .clickable { onSave(content) }.padding(horizontal = 16.dp, vertical = 8.dp)
                ) { Text("保存", fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Medium) }
            }
        }
    }
}
