package org.example.project.ui.screens

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
import org.example.project.ui.components.StatusBarSpacer
import org.example.project.viewmodel.AddWebsiteViewModel

@Composable
fun AddWebsiteScreen(
    panelId: String = "",
    onBackClick: () -> Unit = {},
    onSubmitClick: () -> Unit = {}
) {
    val vm = viewModel<AddWebsiteViewModel>()
    val websiteType by vm.websiteType.collectAsStateWithLifecycle()
    val name by vm.name.collectAsStateWithLifecycle()
    val domains by vm.domains.collectAsStateWithLifecycle()
    val listenPort by vm.listenPort.collectAsStateWithLifecycle()
    val path by vm.path.collectAsStateWithLifecycle()
    val php by vm.php.collectAsStateWithLifecycle()
    val proxy by vm.proxy.collectAsStateWithLifecycle()
    val remark by vm.remark.collectAsStateWithLifecycle()
    val createDb by vm.createDb.collectAsStateWithLifecycle()
    val dbType by vm.dbType.collectAsStateWithLifecycle()
    val dbName by vm.dbName.collectAsStateWithLifecycle()
    val dbUser by vm.dbUser.collectAsStateWithLifecycle()
    val dbPassword by vm.dbPassword.collectAsStateWithLifecycle()
    val installedPhp by vm.installedPhp.collectAsStateWithLifecycle()
    val isLoadingEnv by vm.isLoadingEnv.collectAsStateWithLifecycle()
    val submitStatus by vm.submitStatus.collectAsStateWithLifecycle()

    val isSubmitting = submitStatus is AddWebsiteViewModel.SubmitStatus.Loading
    val submitError = (submitStatus as? AddWebsiteViewModel.SubmitStatus.Error)?.message

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
            Text("添加网站", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF18181B))
            Box(modifier = Modifier.size(36.dp))
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 网站类型
            item {
                WebsiteFormSection("网站类型") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("php" to "PHP", "static" to "静态", "proxy" to "反向代理").forEach { (key, label) ->
                            val isSelected = websiteType == key
                            Box(
                                modifier = Modifier
                                    .height(36.dp)
                                    .background(
                                        if (isSelected) Color(0xFF2563EB) else Color(0xFFF3F4F6),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { vm.websiteType.value = key }
                                    .padding(horizontal = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else Color(0xFF71717A)
                                )
                            }
                        }
                    }
                }
            }

            // 网站名（标识）
            item {
                WebsiteFormSection("网站名称（仅字母/数字/下划线）") {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { vm.name.value = it },
                        placeholder = { Text("my-website") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // 域名
            item {
                WebsiteFormSection("域名（多个用逗号分隔）") {
                    OutlinedTextField(
                        value = domains,
                        onValueChange = { vm.domains.value = it },
                        placeholder = { Text("example.com, www.example.com") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // 监听端口
            item {
                WebsiteFormSection("监听端口") {
                    OutlinedTextField(
                        value = listenPort,
                        onValueChange = { vm.listenPort.value = it },
                        placeholder = { Text("80") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // 运行目录（可选）
            item {
                WebsiteFormSection("运行目录（留空使用默认路径）") {
                    OutlinedTextField(
                        value = path,
                        onValueChange = { vm.path.value = it },
                        placeholder = { Text("/www/wwwroot/my-website/public") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // PHP 版本（type=php）
            if (websiteType == "php") {
                item {
                    WebsiteFormSection("PHP 版本") {
                        if (isLoadingEnv) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else if (installedPhp.isEmpty()) {
                            Text("未检测到已安装的 PHP", fontSize = 13.sp, color = Color(0xFFA1A1AA))
                        } else {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                installedPhp.forEach { version ->
                                    val isSelected = php == version
                                    Box(
                                        modifier = Modifier
                                            .height(36.dp)
                                            .background(
                                                if (isSelected) Color(0xFF2563EB) else Color(0xFFF3F4F6),
                                                RoundedCornerShape(8.dp)
                                            )
                                            .clickable { vm.php.value = version }
                                            .padding(horizontal = 12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "PHP $version",
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
            }

            // 反代地址（type=proxy）
            if (websiteType == "proxy") {
                item {
                    WebsiteFormSection("反向代理地址") {
                        OutlinedTextField(
                            value = proxy,
                            onValueChange = { vm.proxy.value = it },
                            placeholder = { Text("http://127.0.0.1:3000") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }

            // 创建数据库
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "创建数据库",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF71717A),
                        letterSpacing = 1.sp
                    )
                    Switch(
                        checked = createDb,
                        onCheckedChange = { vm.createDb.value = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF2563EB),
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = Color(0xFFE4E4E7)
                        )
                    )
                }
            }

            if (createDb) {
                item {
                    WebsiteFormSection("数据库类型") {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("mysql", "postgresql").forEach { key ->
                                val isSelected = dbType == key
                                Box(
                                    modifier = Modifier
                                        .height(36.dp)
                                        .background(
                                            if (isSelected) Color(0xFF2563EB) else Color(0xFFF3F4F6),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable { vm.dbType.value = key }
                                        .padding(horizontal = 16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = key,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else Color(0xFF71717A)
                                    )
                                }
                            }
                        }
                    }
                }
                item {
                    WebsiteFormSection("数据库名") {
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
                item {
                    WebsiteFormSection("数据库用户名") {
                        OutlinedTextField(
                            value = dbUser,
                            onValueChange = { vm.dbUser.value = it },
                            placeholder = { Text("db_user") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
                item {
                    WebsiteFormSection("数据库密码") {
                        OutlinedTextField(
                            value = dbPassword,
                            onValueChange = { vm.dbPassword.value = it },
                            placeholder = { Text("password") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }

            // 备注
            item {
                WebsiteFormSection("备注（可选）") {
                    OutlinedTextField(
                        value = remark,
                        onValueChange = { vm.remark.value = it },
                        placeholder = { Text("备注") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
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
                    Text("提交", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun WebsiteFormSection(label: String, content: @Composable () -> Unit) {
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
