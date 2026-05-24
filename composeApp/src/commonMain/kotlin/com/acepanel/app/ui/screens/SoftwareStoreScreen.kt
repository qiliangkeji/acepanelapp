package com.acepanel.app.ui.screens

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
import com.acepanel.app.data.AppChannelInfo
import com.acepanel.app.data.AppDetailItem
import com.acepanel.app.data.InstalledEnvironment
import com.acepanel.app.feedback.FeedbackCenter
import com.acepanel.app.ui.components.AnimatedAppDialog
import com.acepanel.app.ui.components.SegmentedTabs
import com.acepanel.app.ui.components.StatusBarSpacer
import com.acepanel.app.ui.components.rememberTabBackStack
import com.acepanel.app.viewmodel.SoftwareStoreViewModel

private data class AppConfigTarget(
    val title: String,
    val filePath: String
)

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
    val environment by vm.environment.collectAsStateWithLifecycle()
    val sectionStack = rememberTabBackStack()
    val selectedSection = sectionStack.current
    var editingConfig by remember { mutableStateOf<AppConfigTarget?>(null) }
    var editingConfigReturnSection by remember { mutableStateOf<Int?>(null) }
    var installTarget by remember { mutableStateOf<AppDetailItem?>(null) }
    var uninstallTarget by remember { mutableStateOf<AppDetailItem?>(null) }

    LaunchedEffect(panelId) {
        if (panelId.isNotEmpty()) vm.init(panelId)
    }

    // 本地过滤
    val displayApps = vm.filteredApps
    val installedApps = vm.installedApps

    installTarget?.let { app ->
        InstallVersionDialog(
            app = app,
            isInstalling = operatingSlug == app.slug,
            onDismiss = { installTarget = null },
            onInstall = { channel ->
                installTarget = null
                vm.installApp(app.slug, channel)
            }
        )
    }

    uninstallTarget?.let { app ->
        UninstallConfirmDialog(
            app = app,
            isUninstalling = operatingSlug == app.slug,
            onDismiss = { uninstallTarget = null },
            onConfirm = {
                uninstallTarget = null
                vm.uninstallApp(app.slug)
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

            SegmentedTabs(
                labels = listOf("商店", "已安装", "配置编辑"),
                selectedIndex = selectedSection,
                onSelected = { index ->
                    sectionStack.select(index)
                    vm.setSection(index)
                    when (index) {
                        0 -> vm.loadApps()
                        1 -> vm.loadInstalledApps()
                    }
                    if (index != 2) editingConfig = null
                    if (index != 2) editingConfigReturnSection = null
                }
            )

            if (selectedSection == 0) {
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
        }

        // Categories
        if (selectedSection == 0) {
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

        when (selectedSection) {
            0 -> SoftwareStoreList(
                panelId = panelId,
                apps = displayApps,
                isLoading = isLoading,
                operatingSlug = operatingSlug,
                onInstall = { app ->
                    when {
                        app.channels.isEmpty() -> {
                            FeedbackCenter.warning("无法安装", "${app.name} 没有可用版本")
                        }
                        app.channels.size == 1 -> {
                            installTarget = app
                        }
                        else -> {
                            installTarget = app
                        }
                    }
                },
                onUpdate = { app -> vm.updateApp(app.slug) },
                onUninstall = { app -> uninstallTarget = app }
            )
            1 -> InstalledAppList(
                panelId = panelId,
                installedApps = installedApps,
                environment = environment,
                operatingSlug = operatingSlug,
                onEditConfig = { target ->
                    editingConfig = target
                    editingConfigReturnSection = selectedSection
                    sectionStack.select(2)
                },
                onUpdate = { app -> vm.updateApp(app.slug) },
                onUninstall = { app -> uninstallTarget = app }
            )
            2 -> SoftwareConfigEditor(
                panelId = panelId,
                target = editingConfig,
                installedApps = installedApps,
                environment = environment,
                onSelectTarget = {
                    editingConfig = it
                    editingConfigReturnSection = null
                }
            )
        }
    }
}

@Composable
private fun InstallVersionDialog(
    app: AppDetailItem,
    isInstalling: Boolean,
    onDismiss: () -> Unit,
    onInstall: (String) -> Unit
) {
    val channels = app.channels
    var selectedChannelSlug by remember(app.slug, channels) {
        mutableStateOf(channels.firstOrNull()?.slug.orEmpty())
    }
    val selectedChannel = channels.firstOrNull { it.slug == selectedChannelSlug }
    val releaseLog = selectedChannel?.log.orEmpty()

    AnimatedAppDialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(16.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "安装 ${app.name}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF18181B)
                )
                Text(
                    text = "选择要安装的版本",
                    fontSize = 13.sp,
                    color = Color(0xFF71717A)
                )
            }

            if (channels.isEmpty()) {
                Text("暂无可安装版本", fontSize = 14.sp, color = Color(0xFFEF4444))
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    channels.forEach { channel ->
                        AppChannelOption(
                            channel = channel,
                            selected = channel.slug == selectedChannelSlug,
                            onClick = { selectedChannelSlug = channel.slug }
                        )
                    }
                }
            }

            if (releaseLog.isNotBlank()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF9FAFB), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "发布日志",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF71717A)
                    )
                    Text(
                        text = releaseLog,
                        fontSize = 12.sp,
                        color = Color(0xFF3F3F46),
                        lineHeight = 17.sp,
                        maxLines = 8
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedChannel?.version?.takeIf { it.isNotBlank() }?.let { "版本 $it" } ?: "",
                    fontSize = 12.sp,
                    color = Color(0xFF71717A),
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .height(38.dp)
                        .background(Color(0xFFF4F4F5), RoundedCornerShape(10.dp))
                        .clickable(enabled = !isInstalling, onClick = onDismiss)
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("取消", fontSize = 14.sp, color = Color(0xFF71717A))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .height(38.dp)
                        .background(
                            if (selectedChannelSlug.isNotBlank() && !isInstalling) Color(0xFF2563EB) else Color(0xFFBFDBFE),
                            RoundedCornerShape(10.dp)
                        )
                        .clickable(enabled = selectedChannelSlug.isNotBlank() && !isInstalling) {
                            onInstall(selectedChannelSlug)
                        }
                        .padding(horizontal = 18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isInstalling) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                    } else {
                        Text("安装", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun AppChannelOption(
    channel: AppChannelInfo,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (selected) Color(0xFFEFF6FF) else Color(0xFFF9FAFB), RoundedCornerShape(12.dp))
            .border(
                1.5.dp,
                if (selected) Color(0xFF2563EB) else Color(0xFFE4E4E7),
                RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = channel.name.ifBlank { channel.slug },
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF18181B)
            )
            val details = listOfNotNull(
                channel.version.takeIf { it.isNotBlank() }?.let { "版本 $it" },
                channel.panel.takeIf { it.isNotBlank() }?.let { "要求面板 $it" }
            ).joinToString(" · ")
            if (details.isNotEmpty()) {
                Text(details, fontSize = 12.sp, color = Color(0xFF71717A))
            }
        }
        if (selected) {
            Text("已选", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF2563EB))
        }
    }
}

@Composable
private fun UninstallConfirmDialog(
    app: AppDetailItem,
    isUninstalling: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AnimatedAppDialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(16.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "卸载 ${app.name}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF18181B)
                )
                Text(
                    text = "确认卸载后，应用服务和相关安装文件会由面板执行清理。",
                    fontSize = 13.sp,
                    color = Color(0xFF71717A),
                    lineHeight = 18.sp
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .height(38.dp)
                        .background(Color(0xFFF4F4F5), RoundedCornerShape(10.dp))
                        .clickable(enabled = !isUninstalling, onClick = onDismiss)
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("取消", fontSize = 14.sp, color = Color(0xFF71717A))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .height(38.dp)
                        .background(
                            if (isUninstalling) Color(0xFFFCA5A5) else Color(0xFFDC2626),
                            RoundedCornerShape(10.dp)
                        )
                        .clickable(enabled = !isUninstalling, onClick = onConfirm)
                        .padding(horizontal = 18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isUninstalling) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                    } else {
                        Text("确认卸载", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun SoftwareStoreList(
    panelId: String,
    apps: List<AppDetailItem>,
    isLoading: Boolean,
    operatingSlug: String?,
    onInstall: (AppDetailItem) -> Unit,
    onUpdate: (AppDetailItem) -> Unit,
    onUninstall: (AppDetailItem) -> Unit
) {
    if (apps.isEmpty() && !isLoading) {
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
            items(apps) { app ->
                AppDetailCard(
                    app = app,
                    isOperating = operatingSlug == app.slug,
                    onInstall = { onInstall(app) },
                    onUpdate = { onUpdate(app) },
                    onUninstall = { onUninstall(app) }
                )
            }
        }
    }
}

@Composable
private fun InstalledAppList(
    panelId: String,
    installedApps: List<AppDetailItem>,
    environment: InstalledEnvironment?,
    operatingSlug: String?,
    onEditConfig: (AppConfigTarget) -> Unit,
    onUpdate: (AppDetailItem) -> Unit,
    onUninstall: (AppDetailItem) -> Unit
) {
    val configTargets = buildAppConfigTargets(installedApps, environment)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (installedApps.isEmpty() && configTargets.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (panelId.isEmpty()) "请先选择面板" else "暂无已安装应用",
                        fontSize = 14.sp,
                        color = Color(0xFFA1A1AA)
                    )
                }
            }
        }

        if (configTargets.isNotEmpty()) {
            item {
                Text(
                    "应用环境",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF71717A),
                    letterSpacing = 1.sp
                )
            }
            items(configTargets) { target ->
                InstalledEnvironmentCard(target = target, onEditConfig = { onEditConfig(target) })
            }
        }

        if (installedApps.isNotEmpty()) {
            item {
                Text(
                    "已安装应用",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF71717A),
                    letterSpacing = 1.sp
                )
            }
            items(installedApps) { app ->
                AppDetailCard(
                    app = app,
                    isOperating = operatingSlug == app.slug,
                    onUpdate = { onUpdate(app) },
                    onUninstall = { onUninstall(app) }
                )
            }
        }
    }
}

@Composable
private fun InstalledEnvironmentCard(
    target: AppConfigTarget,
    onEditConfig: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, Color(0xFFE4E4E7), RoundedCornerShape(16.dp))
            .clickable(onClick = onEditConfig)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(target.title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF18181B))
            Text(target.filePath, fontSize = 12.sp, color = Color(0xFF71717A))
        }
        Text("配置", fontSize = 13.sp, color = Color(0xFF2563EB), fontWeight = FontWeight.Medium)
    }
}

private fun buildAppConfigTargets(
    installedApps: List<AppDetailItem>,
    environment: InstalledEnvironment?
): List<AppConfigTarget> {
    val targets = mutableListOf<AppConfigTarget>()
    val aceRoot = "/opt/ace"

    fun add(title: String, path: String) {
        if (targets.none { it.filePath == path }) targets += AppConfigTarget(title, path)
    }

    fun addWebserverConfig(slug: String, title: String? = null) {
        when (slug.lowercase()) {
            "nginx", "openresty" -> add(title ?: "Nginx 主配置", "$aceRoot/server/nginx/conf/nginx.conf")
            "apache" -> add(title ?: "Apache 主配置", "$aceRoot/server/apache/conf/httpd.conf")
        }
    }

    fun addDatabaseConfig(slug: String, title: String? = null) {
        when (slug.lowercase()) {
            "mysql" -> add(title ?: "MySQL 配置", "$aceRoot/server/mysql/conf/my.cnf")
            "mariadb" -> add(title ?: "MariaDB 配置", "$aceRoot/server/mysql/conf/my.cnf")
            "percona" -> add(title ?: "Percona 配置", "$aceRoot/server/mysql/conf/my.cnf")
            "postgresql" -> add(title ?: "PostgreSQL 配置", "$aceRoot/server/postgresql/data/postgresql.conf")
            "redis" -> add(title ?: "Redis 配置", "$aceRoot/server/redis/redis.conf")
            "valkey" -> add(title ?: "Valkey 配置", "$aceRoot/server/valkey/valkey.conf")
        }
    }

    fun addPhpConfig(version: Int, label: String) {
        add("$label php.ini", "$aceRoot/server/php/$version/etc/php.ini")
        add("$label php-fpm.conf", "$aceRoot/server/php/$version/etc/php-fpm.conf")
    }

    val webserver = environment?.webserver?.lowercase().orEmpty()
    when {
        webserver.contains("openresty") -> addWebserverConfig("openresty", "OpenResty 主配置")
        webserver.contains("nginx") -> addWebserverConfig("nginx")
        webserver.contains("apache") -> addWebserverConfig("apache")
    }

    environment?.php.orEmpty().forEach { php ->
        addPhpConfig(php.value, "PHP ${php.label.ifEmpty { php.value.toString() }}")
    }

    environment?.db.orEmpty().forEach { db ->
        addDatabaseConfig(db.value)
    }

    installedApps.forEach { app ->
        addWebserverConfig(app.slug)
        addDatabaseConfig(app.slug)
        if (app.slug.startsWith("php")) {
            val version = app.slug.filter { it.isDigit() }.takeIf { it.isNotEmpty() }
            if (version != null) addPhpConfig(version.toInt(), app.name)
        }
    }

    return targets
}

@Composable
private fun SoftwareConfigEditor(
    panelId: String,
    target: AppConfigTarget?,
    installedApps: List<AppDetailItem>,
    environment: InstalledEnvironment?,
    onSelectTarget: (AppConfigTarget) -> Unit
) {
    val targets = buildAppConfigTargets(installedApps, environment)
    if (target == null) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (targets.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
                        Text("暂无可编辑的应用配置", fontSize = 14.sp, color = Color(0xFFA1A1AA))
                    }
                }
            } else {
                item {
                    Text("选择要编辑的配置", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF71717A), letterSpacing = 1.sp)
                }
                items(targets) { item ->
                    InstalledEnvironmentCard(target = item, onEditConfig = { onSelectTarget(item) })
                }
            }
        }
    } else {
        ConfigEditorScreen(
            panelId = panelId,
            filePath = target.filePath,
            fileName = target.title,
            showStatusBarSpacer = false,
            showBackButton = false
        )
    }
}

@Composable
fun AppDetailCard(
    app: AppDetailItem,
    isOperating: Boolean = false,
    onInstall: () -> Unit = {},
    onUpdate: () -> Unit = {},
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
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (app.installed) {
                        if (app.update_exist) {
                            AppActionButton(
                                text = "更新",
                                background = Color(0xFFFEF3C7),
                                foreground = Color(0xFFD97706),
                                onClick = onUpdate
                            )
                        }
                        AppActionButton(
                            text = "卸载",
                            background = Color(0xFFFEE2E2),
                            foreground = Color(0xFFDC2626),
                            onClick = onUninstall
                        )
                    } else {
                        AppActionButton(
                            text = "安装",
                            background = Color(0xFF2563EB),
                            foreground = Color.White,
                            onClick = onInstall
                        )
                    }
                }
            }
        }

        if (app.description.isNotEmpty()) {
            Text(text = app.description, fontSize = 13.sp, color = Color(0xFF71717A))
        }
    }
}

@Composable
private fun AppActionButton(
    text: String,
    background: Color,
    foreground: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .height(28.dp)
            .background(background, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = foreground)
    }
}
