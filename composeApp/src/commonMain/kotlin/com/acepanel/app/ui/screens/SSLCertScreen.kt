package com.acepanel.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import com.acepanel.app.data.AcmeAccountItem
import com.acepanel.app.data.CertListItem
import com.acepanel.app.data.DnsProviderItem
import com.acepanel.app.ui.components.AnimatedAppDialog
import com.acepanel.app.ui.components.SegmentedTabs
import com.acepanel.app.ui.components.StatusBarSpacer
import com.acepanel.app.ui.components.rememberTabBackStack
import com.acepanel.app.viewmodel.SSLCertViewModel

@Composable
fun SSLCertScreen(
    panelId: String = "",
    onBackClick: () -> Unit = {},
    onAddClick: () -> Unit = {}
) {
    val vm: SSLCertViewModel = viewModel()
    val certs by vm.certs.collectAsStateWithLifecycle()
    val isLoading by vm.isLoading.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    val actionId by vm.actionId.collectAsStateWithLifecycle()
    val actionError by vm.actionError.collectAsStateWithLifecycle()
    val acmeAccounts by vm.acmeAccounts.collectAsStateWithLifecycle()
    val dnsProviders by vm.dnsProviders.collectAsStateWithLifecycle()
    val showCreateDialog by vm.showCreateDialog.collectAsStateWithLifecycle()
    val showUploadDialog by vm.showUploadDialog.collectAsStateWithLifecycle()
    val tabStack = rememberTabBackStack()
    val selectedTab = tabStack.current

    LaunchedEffect(panelId) {
        if (panelId.isNotEmpty()) vm.init(panelId)
    }

    if (showCreateDialog) {
        CreateCertDialog(
            acmeAccounts = acmeAccounts,
            dnsProviders = dnsProviders,
            actionError = actionError,
            onDismiss = { vm.hideCreate() },
            onCreate = { type, domains, autoRenewal, accountId, dnsId ->
                vm.createCert(type, domains, autoRenewal, accountId, dnsId)
            }
        )
    }

    if (showUploadDialog) {
        UploadCertDialog(
            actionError = actionError,
            onDismiss = { vm.hideUpload() },
            onUpload = { cert, key -> vm.uploadCert(cert, key) }
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
                        .clickable(onClick = onBackClick),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "‹", fontSize = 20.sp, color = Color(0xFF18181B))
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "SSL 证书",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF18181B)
                )
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color(0xFF2563EB))
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (selectedTab == 0) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.5.dp, Color(0xFFE4E4E7), RoundedCornerShape(10.dp))
                            .clickable { vm.load() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "↻", fontSize = 18.sp, color = Color(0xFF18181B))
                    }
                    // 上传证书
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.5.dp, Color(0xFFE4E4E7), RoundedCornerShape(10.dp))
                            .clickable { vm.showUpload() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "↑", fontSize = 16.sp, color = Color(0xFF18181B))
                    }
                    // 新建证书
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF2563EB))
                            .clickable {
                                vm.loadCertOptions()
                                vm.showCreate()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "+", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                } else {
                    Spacer(modifier = Modifier.size(36.dp))
                }
            }
        }

        SegmentedTabs(
            labels = listOf("证书", "ACME/DNS配置", "用户管理"),
            selectedIndex = selectedTab,
            onSelected = { tabStack.select(it) },
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
        )

        when (selectedTab) {
            0 -> SSLCertListContent(
                panelId = panelId,
                certs = certs,
                isLoading = isLoading,
                error = error,
                actionError = actionError,
                actionId = actionId,
                onObtainAuto = { vm.obtainAuto(it) },
                onObtainSelf = { vm.obtainSelf(it) },
                onRenew = { vm.renew(it) },
                onDelete = { vm.delete(it) }
            )
            1 -> CertConfigScreen(
                panelId = panelId,
                showStatusBarSpacer = false,
                showBackButton = false
            )
            2 -> UserManagementScreen(
                panelId = panelId,
                showStatusBarSpacer = false,
                showBackButton = false
            )
        }
    }
}

@Composable
private fun SSLCertListContent(
    panelId: String,
    certs: List<CertListItem>,
    isLoading: Boolean,
    error: String?,
    actionError: String?,
    actionId: Long?,
    onObtainAuto: (Long) -> Unit,
    onObtainSelf: (Long) -> Unit,
    onRenew: (Long) -> Unit,
    onDelete: (Long) -> Unit
) {
    if (error != null) {
        Text(
            text = "加载失败: $error",
            fontSize = 12.sp,
            color = Color(0xFFEF4444),
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
        )
    }
    if (actionError != null) {
        Text(
            text = actionError,
            fontSize = 12.sp,
            color = Color(0xFFEF4444),
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
        )
    }

    if (certs.isEmpty() && !isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = if (panelId.isEmpty()) "请先选择面板" else "暂无证书",
                fontSize = 14.sp, color = Color(0xFFA1A1AA)
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(certs) { cert ->
                CertCard(
                    cert = cert,
                    isActing = actionId == cert.id,
                    onObtainAuto = { onObtainAuto(cert.id) },
                    onObtainSelf = { onObtainSelf(cert.id) },
                    onRenew = { onRenew(cert.id) },
                    onDelete = { onDelete(cert.id) }
                )
            }
        }
    }
}

@Composable
private fun CertCard(
    cert: CertListItem,
    isActing: Boolean,
    onObtainAuto: () -> Unit,
    onObtainSelf: () -> Unit,
    onRenew: () -> Unit,
    onDelete: () -> Unit
) {
    val hasCert = cert.cert.isNotEmpty()
    val hasExpiry = cert.not_after.isNotEmpty()

    val (statusColor, statusBg, statusLabel) = when {
        hasCert && hasExpiry -> Triple(Color(0xFF059669), Color(0xFFECFDF5), "有效")
        !hasCert -> Triple(Color(0xFF71717A), Color(0xFFF3F4F6), "待签发")
        else -> Triple(Color(0xFFEF4444), Color(0xFFFEF2F2), "无效")
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.5.dp, Color(0xFFE4E4E7), RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = cert.domains.firstOrNull() ?: "未知域名",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF18181B)
                )
                if (cert.domains.size > 1) {
                    Text(text = "+${cert.domains.size - 1} 个域名", fontSize = 12.sp, color = Color(0xFF71717A))
                }
            }
            if (isActing) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color(0xFF2563EB))
            } else {
                Box(
                    modifier = Modifier
                        .height(24.dp)
                        .background(statusBg, RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(statusLabel, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = statusColor)
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (cert.issuer.isNotEmpty()) {
                Text(text = "签发机构: ${cert.issuer}", fontSize = 12.sp, color = Color(0xFF71717A))
            }
            if (cert.not_after.isNotEmpty()) {
                Text(text = "到期时间: ${cert.not_after.take(10)}", fontSize = 12.sp, color = Color(0xFF71717A))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (cert.type.isNotEmpty()) {
                    Box(
                        modifier = Modifier.background(Color(0xFFF3F4F6), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) { Text(cert.type, fontSize = 10.sp, color = Color(0xFF71717A)) }
                }
                if (cert.auto_renewal) {
                    Box(
                        modifier = Modifier.background(Color(0xFFECFDF5), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) { Text("自动续期", fontSize = 10.sp, color = Color(0xFF059669)) }
                }
            }
        }

        HorizontalDivider(thickness = 1.dp, color = Color(0xFFE4E4E7))

        // 操作按钮行1：签发
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CertActionBtn("ACME签发", Color(0xFF2563EB), !isActing, onObtainAuto, Modifier.weight(1f))
            CertActionBtn("自签名", Color(0xFF7C3AED), !isActing, onObtainSelf, Modifier.weight(1f))
        }
        // 操作按钮行2：续期 + 删除
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CertActionBtn("续期", Color(0xFF059669), !isActing, onRenew, Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, Color(0xFFEF4444), RoundedCornerShape(8.dp))
                    .clickable(enabled = !isActing, onClick = onDelete),
                contentAlignment = Alignment.Center
            ) {
                Text("删除", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFFEF4444))
            }
        }
    }
}

@Composable
private fun CertActionBtn(
    label: String,
    color: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (enabled) color else Color(0xFFE4E4E7))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color.White)
    }
}

@Composable
private fun CreateCertDialog(
    acmeAccounts: List<AcmeAccountItem>,
    dnsProviders: List<DnsProviderItem>,
    actionError: String?,
    onDismiss: () -> Unit,
    onCreate: (String, List<String>, Boolean, Long, Long) -> Unit
) {
    var domainsText by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("P256") }
    var autoRenewal by remember { mutableStateOf(false) }
    var selectedAccountId by remember(acmeAccounts) { mutableStateOf(acmeAccounts.firstOrNull()?.id ?: 0L) }
    var selectedDnsId by remember(dnsProviders) { mutableStateOf(dnsProviders.firstOrNull()?.id ?: 0L) }
    var showAccountMenu by remember { mutableStateOf(false) }
    var showDnsMenu by remember { mutableStateOf(false) }

    val typeOptions = listOf("P256", "P384", "2048", "3072", "4096")
    val selectedAccount = acmeAccounts.firstOrNull { it.id == selectedAccountId }
    val selectedDns = dnsProviders.firstOrNull { it.id == selectedDnsId }

    AnimatedAppDialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("新建证书", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF18181B))

            OutlinedTextField(
                value = domainsText,
                onValueChange = { domainsText = it },
                label = { Text("域名（多个用逗号分隔）") },
                placeholder = { Text("example.com, *.example.com") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("密钥类型", fontSize = 12.sp, color = Color(0xFF71717A))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    typeOptions.forEach { t ->
                        Box(
                            modifier = Modifier
                                .height(30.dp)
                                .background(
                                    if (selectedType == t) Color(0xFF2563EB) else Color(0xFFF3F4F6),
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedType = t }
                                .padding(horizontal = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(t, fontSize = 12.sp, color = if (selectedType == t) Color.White else Color(0xFF71717A))
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("ACME 账号", fontSize = 12.sp, color = Color(0xFF71717A))
                Box {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFFE4E4E7), RoundedCornerShape(8.dp))
                            .clickable(enabled = acmeAccounts.isNotEmpty()) { showAccountMenu = true }
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = selectedAccount?.let { "${it.email} (${it.ca})" } ?: "未配置 ACME 账号",
                            fontSize = 13.sp,
                            color = if (selectedAccount != null) Color(0xFF18181B) else Color(0xFFA1A1AA)
                        )
                    }
                    DropdownMenu(expanded = showAccountMenu, onDismissRequest = { showAccountMenu = false }) {
                        acmeAccounts.forEach { account ->
                            DropdownMenuItem(
                                text = { Text("${account.email} (${account.ca}, ${account.key_type})") },
                                onClick = {
                                    selectedAccountId = account.id
                                    showAccountMenu = false
                                }
                            )
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("DNS 凭据", fontSize = 12.sp, color = Color(0xFF71717A))
                Box {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFFE4E4E7), RoundedCornerShape(8.dp))
                            .clickable(enabled = dnsProviders.isNotEmpty()) { showDnsMenu = true }
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = selectedDns?.let { "${it.name} (${it.type})" } ?: "不使用 DNS 验证",
                            fontSize = 13.sp,
                            color = if (selectedDns != null) Color(0xFF18181B) else Color(0xFFA1A1AA)
                        )
                    }
                    DropdownMenu(expanded = showDnsMenu, onDismissRequest = { showDnsMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("不使用 DNS 验证") },
                            onClick = {
                                selectedDnsId = 0
                                showDnsMenu = false
                            }
                        )
                        dnsProviders.forEach { dns ->
                            DropdownMenuItem(
                                text = { Text("${dns.name} (${dns.type})") },
                                onClick = {
                                    selectedDnsId = dns.id
                                    showDnsMenu = false
                                }
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("自动续期", fontSize = 13.sp, color = Color(0xFF18181B))
                Switch(
                    checked = autoRenewal,
                    onCheckedChange = { autoRenewal = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF2563EB),
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = Color(0xFFE4E4E7)
                    )
                )
            }

            if (actionError != null) {
                Text(actionError, fontSize = 12.sp, color = Color(0xFFEF4444))
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("取消", color = Color(0xFF71717A)) }
                Spacer(Modifier.width(8.dp))
                val domains = domainsText.split(",").map { it.trim() }.filter { it.isNotBlank() }
                val canSubmit = domains.isNotEmpty()
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (canSubmit) Color(0xFF2563EB) else Color(0xFF71717A))
                        .clickable(enabled = canSubmit) {
                            onCreate(
                                selectedType,
                                domains,
                                autoRenewal,
                                selectedAccountId,
                                selectedDnsId
                            )
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text("创建", fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun UploadCertDialog(
    actionError: String?,
    onDismiss: () -> Unit,
    onUpload: (String, String) -> Unit
) {
    var certPem by remember { mutableStateOf("") }
    var keyPem by remember { mutableStateOf("") }

    AnimatedAppDialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("上传证书", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF18181B))
            Text("粘贴 PEM 格式的证书和私钥", fontSize = 12.sp, color = Color(0xFF71717A))

            OutlinedTextField(
                value = certPem,
                onValueChange = { certPem = it },
                label = { Text("证书（cert.pem）") },
                placeholder = { Text("-----BEGIN CERTIFICATE-----") },
                modifier = Modifier.fillMaxWidth().height(120.dp),
                maxLines = 6
            )

            OutlinedTextField(
                value = keyPem,
                onValueChange = { keyPem = it },
                label = { Text("私钥（key.pem）") },
                placeholder = { Text("-----BEGIN PRIVATE KEY-----") },
                modifier = Modifier.fillMaxWidth().height(120.dp),
                maxLines = 6
            )

            if (actionError != null) {
                Text(actionError, fontSize = 12.sp, color = Color(0xFFEF4444))
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("取消", color = Color(0xFF71717A)) }
                Spacer(Modifier.width(8.dp))
                val canSubmit = certPem.isNotBlank() && keyPem.isNotBlank()
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (canSubmit) Color(0xFF2563EB) else Color(0xFF71717A))
                        .clickable(enabled = canSubmit) { onUpload(certPem.trim(), keyPem.trim()) }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text("上传", fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}
