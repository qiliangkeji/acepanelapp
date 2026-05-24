package com.acepanel.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
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
import com.acepanel.app.data.DnsProviderItem
import com.acepanel.app.ui.components.SegmentedTabs
import com.acepanel.app.ui.components.StatusBarSpacer
import com.acepanel.app.ui.components.rememberTabBackStack
import com.acepanel.app.viewmodel.CertConfigViewModel

private val CA_OPTIONS = listOf("letsencrypt", "zerossl", "google", "googlecn", "litessl", "sslcom")
private val KEY_TYPE_OPTIONS = listOf("P256", "P384", "2048", "3072", "4096")
private val DNS_TYPE_OPTIONS = listOf(
    "aliyun", "tencent", "huawei", "westcn",
    "cloudflare", "gcore", "porkbun", "namesilo", "cloudns"
)

@Composable
fun CertConfigScreen(
    panelId: String,
    onBack: () -> Unit = {},
    showStatusBarSpacer: Boolean = true,
    showBackButton: Boolean = true
) {
    val vm: CertConfigViewModel = viewModel()
    val acmeAccounts by vm.acmeAccounts.collectAsStateWithLifecycle()
    val dnsProviders by vm.dnsProviders.collectAsStateWithLifecycle()
    val isAcmeLoading by vm.isAcmeLoading.collectAsStateWithLifecycle()
    val isDnsLoading by vm.isDnsLoading.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    val actionError by vm.actionError.collectAsStateWithLifecycle()

    LaunchedEffect(panelId) { vm.init(panelId) }

    val tabStack = rememberTabBackStack()
    val selectedTab = tabStack.current
    var showAddAcme by remember { mutableStateOf(false) }
    var showAddDns by remember { mutableStateOf(false) }
    var editAcmeTarget by remember { mutableStateOf<AcmeAccountItem?>(null) }
    var editDnsTarget by remember { mutableStateOf<DnsProviderItem?>(null) }
    var deleteAcmeTarget by remember { mutableStateOf<AcmeAccountItem?>(null) }
    var deleteDnsTarget by remember { mutableStateOf<DnsProviderItem?>(null) }

    Column(
        modifier = Modifier.fillMaxSize().background(Color.White)
    ) {
        if (showStatusBarSpacer) StatusBarSpacer()

        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showBackButton) {
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
            } else {
                Spacer(modifier = Modifier.size(36.dp))
            }
            Text(
                "ACME / DNS 配置",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF18181B)
            )
            Box(
                modifier = Modifier
                    .height(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF2563EB))
                    .clickable { if (selectedTab == 0) showAddAcme = true else showAddDns = true }
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("+ 添加", fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Medium)
            }
        }

        SegmentedTabs(
            labels = listOf("ACME 账号", "DNS 凭据"),
            selectedIndex = selectedTab,
            onSelected = { tabStack.select(it) },
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 错误提示
        if (error != null || actionError != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFFEF2F2))
                    .padding(12.dp)
            ) {
                Text(
                    "⚠ ${actionError ?: error}",
                    fontSize = 13.sp,
                    color = Color(0xFFEF4444)
                )
            }
        }

        if (selectedTab == 0) {
            // ACME 账号列表
            if (isAcmeLoading) {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF2563EB))
                }
            } else if (acmeAccounts.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                    Text("暂无 ACME 账号", fontSize = 14.sp, color = Color(0xFFA1A1AA))
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(acmeAccounts) { item ->
                        AcmeAccountCard(
                            item = item,
                            onEdit = { editAcmeTarget = item },
                            onDelete = { deleteAcmeTarget = item }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        } else {
            // DNS 凭据列表
            if (isDnsLoading) {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF2563EB))
                }
            } else if (dnsProviders.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                    Text("暂无 DNS 凭据", fontSize = 14.sp, color = Color(0xFFA1A1AA))
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(dnsProviders) { item ->
                        DnsProviderCard(
                            item = item,
                            onEdit = { editDnsTarget = item },
                            onDelete = { deleteDnsTarget = item }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }
    }

    // 添加 ACME 账号弹窗
    if (showAddAcme) {
        AddAcmeDialog(
            onDismiss = { showAddAcme = false },
            onConfirm = { ca, email, keyType, kid, hmac ->
                vm.createAcmeAccount(ca, email, keyType, kid, hmac)
                showAddAcme = false
            }
        )
    }

    // 编辑 ACME 账号弹窗
    editAcmeTarget?.let { target ->
        AddAcmeDialog(
            initial = target,
            onDismiss = { editAcmeTarget = null },
            onConfirm = { ca, email, keyType, kid, hmac ->
                vm.updateAcmeAccount(target.id, ca, email, keyType, kid, hmac)
                editAcmeTarget = null
            }
        )
    }

    // 添加 DNS 凭据弹窗
    if (showAddDns) {
        AddDnsDialog(
            onDismiss = { showAddDns = false },
            onConfirm = { type, name, ak, sk ->
                vm.createDnsProvider(type, name, ak, sk)
                showAddDns = false
            }
        )
    }

    // 编辑 DNS 凭据弹窗
    editDnsTarget?.let { target ->
        AddDnsDialog(
            initial = target,
            onDismiss = { editDnsTarget = null },
            onConfirm = { type, name, ak, sk ->
                vm.updateDnsProvider(target.id, type, name, ak, sk.ifBlank { target.dns_param.sk })
                editDnsTarget = null
            }
        )
    }

    // 删除 ACME 确认
    deleteAcmeTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteAcmeTarget = null },
            title = { Text("删除 ACME 账号", fontWeight = FontWeight.SemiBold) },
            text = { Text("确认删除 ${target.email}（${target.ca}）？") },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteAcmeAccount(target.id)
                    deleteAcmeTarget = null
                }) {
                    Text("删除", color = Color(0xFFEF4444))
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteAcmeTarget = null }) {
                    Text("取消", color = Color(0xFF2563EB))
                }
            }
        )
    }

    // 删除 DNS 确认
    deleteDnsTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteDnsTarget = null },
            title = { Text("删除 DNS 凭据", fontWeight = FontWeight.SemiBold) },
            text = { Text("确认删除 ${target.name}（${target.type}）？") },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteDnsProvider(target.id)
                    deleteDnsTarget = null
                }) {
                    Text("删除", color = Color(0xFFEF4444))
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteDnsTarget = null }) {
                    Text("取消", color = Color(0xFF2563EB))
                }
            }
        )
    }
}

@Composable
private fun AcmeAccountCard(
    item: AcmeAccountItem,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.5.dp, Color(0xFFE4E4E7), RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    item.email,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF18181B)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable(onClick = onEdit)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) { Text("编辑", fontSize = 12.sp, color = Color(0xFF2563EB)) }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable(onClick = onDelete)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) { Text("删除", fontSize = 12.sp, color = Color(0xFFEF4444)) }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CertTag(item.ca, Color(0xFF2563EB), Color(0xFFEFF6FF))
                CertTag(item.key_type, Color(0xFF7C3AED), Color(0xFFF5F3FF))
            }
            if (item.kid.isNotEmpty()) {
                Text("KID: ${item.kid}", fontSize = 11.sp, color = Color(0xFF71717A))
            }
        }
    }
}

@Composable
private fun DnsProviderCard(
    item: DnsProviderItem,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.5.dp, Color(0xFFE4E4E7), RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    item.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF18181B)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable(onClick = onEdit)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) { Text("编辑", fontSize = 12.sp, color = Color(0xFF2563EB)) }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable(onClick = onDelete)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) { Text("删除", fontSize = 12.sp, color = Color(0xFFEF4444)) }
                }
            }
            CertTag(item.type, Color(0xFF059669), Color(0xFFECFDF5))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("AK: ${maskSecret(item.dns_param.ak)}", fontSize = 12.sp, color = Color(0xFF71717A))
                Text("SK: ${maskSecret(item.dns_param.sk)}", fontSize = 12.sp, color = Color(0xFF71717A))
            }
        }
    }
}

private fun maskSecret(s: String): String =
    if (s.length <= 4) "****" else "${s.take(2)}****${s.takeLast(2)}"

@Composable
private fun CertTag(text: String, textColor: Color, bgColor: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(text, fontSize = 12.sp, color = textColor, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun AddAcmeDialog(
    initial: AcmeAccountItem? = null,
    onDismiss: () -> Unit,
    onConfirm: (ca: String, email: String, keyType: String, kid: String, hmac: String) -> Unit
) {
    val isEdit = initial != null
    var ca by remember { mutableStateOf(initial?.ca ?: CA_OPTIONS.first()) }
    var email by remember { mutableStateOf(initial?.email ?: "") }
    var keyType by remember { mutableStateOf(initial?.key_type ?: KEY_TYPE_OPTIONS.first()) }
    var kid by remember { mutableStateOf(initial?.kid ?: "") }
    var hmac by remember { mutableStateOf("") }
    var showCaDropdown by remember { mutableStateOf(false) }
    var showKeyTypeDropdown by remember { mutableStateOf(false) }
    // google/litessl/sslcom 需用户提供 EAB；googlecn/zerossl 后端自动获取无需填写
    val isEab = ca == "google" || ca == "litessl" || ca == "sslcom"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEdit) "编辑 ACME 账号" else "添加 ACME 账号", fontWeight = FontWeight.SemiBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // CA 选择
                Box {
                    OutlinedTextField(
                        value = ca,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("CA 提供商") },
                        modifier = Modifier.fillMaxWidth().clickable { showCaDropdown = true },
                        trailingIcon = { Text("▾", modifier = Modifier.clickable { showCaDropdown = true }) },
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF2563EB),
                            unfocusedBorderColor = Color(0xFFE4E4E7)
                        )
                    )
                    DropdownMenu(
                        expanded = showCaDropdown,
                        onDismissRequest = { showCaDropdown = false }
                    ) {
                        CA_OPTIONS.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = { ca = option; showCaDropdown = false }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("邮箱地址") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF2563EB),
                        unfocusedBorderColor = Color(0xFFE4E4E7)
                    )
                )
                // 密钥类型
                Box {
                    OutlinedTextField(
                        value = keyType,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("密钥类型") },
                        modifier = Modifier.fillMaxWidth().clickable { showKeyTypeDropdown = true },
                        trailingIcon = { Text("▾", modifier = Modifier.clickable { showKeyTypeDropdown = true }) },
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF2563EB),
                            unfocusedBorderColor = Color(0xFFE4E4E7)
                        )
                    )
                    DropdownMenu(
                        expanded = showKeyTypeDropdown,
                        onDismissRequest = { showKeyTypeDropdown = false }
                    ) {
                        KEY_TYPE_OPTIONS.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = { keyType = option; showKeyTypeDropdown = false }
                            )
                        }
                    }
                }
                // EAB 字段（仅 ZeroSSL/SSLcom 需要）
                if (isEab) {
                    Text("EAB 凭据（${ca}）", fontSize = 12.sp, color = Color(0xFF71717A))
                    OutlinedTextField(
                        value = kid,
                        onValueChange = { kid = it },
                        label = { Text("KID") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF2563EB),
                            unfocusedBorderColor = Color(0xFFE4E4E7)
                        )
                    )
                    OutlinedTextField(
                        value = hmac,
                        onValueChange = { hmac = it },
                        label = { Text("HMAC Key") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF2563EB),
                            unfocusedBorderColor = Color(0xFFE4E4E7)
                        )
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (email.isNotBlank()) onConfirm(ca, email, keyType, kid, hmac) },
                enabled = email.isNotBlank()
            ) {
                Text(if (isEdit) "保存" else "创建", color = Color(0xFF2563EB))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = Color(0xFF71717A))
            }
        }
    )
}

@Composable
private fun AddDnsDialog(
    initial: DnsProviderItem? = null,
    onDismiss: () -> Unit,
    onConfirm: (type: String, name: String, ak: String, sk: String) -> Unit
) {
    val isEdit = initial != null
    var type by remember { mutableStateOf(initial?.type ?: DNS_TYPE_OPTIONS.first()) }
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var ak by remember { mutableStateOf(initial?.dns_param?.ak ?: "") }
    var sk by remember { mutableStateOf("") }  // sk 不预填（安全）；编辑时留空表示保留原值
    var showTypeDropdown by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEdit) "编辑 DNS 凭据" else "添加 DNS 凭据", fontWeight = FontWeight.SemiBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Box {
                    OutlinedTextField(
                        value = type,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("DNS 提供商") },
                        modifier = Modifier.fillMaxWidth().clickable { showTypeDropdown = true },
                        trailingIcon = { Text("▾", modifier = Modifier.clickable { showTypeDropdown = true }) },
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF2563EB),
                            unfocusedBorderColor = Color(0xFFE4E4E7)
                        )
                    )
                    DropdownMenu(
                        expanded = showTypeDropdown,
                        onDismissRequest = { showTypeDropdown = false }
                    ) {
                        DNS_TYPE_OPTIONS.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = { type = option; showTypeDropdown = false }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("名称（备注）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF2563EB),
                        unfocusedBorderColor = Color(0xFFE4E4E7)
                    )
                )
                OutlinedTextField(
                    value = ak,
                    onValueChange = { ak = it },
                    label = { Text("Access Key (AK)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF2563EB),
                        unfocusedBorderColor = Color(0xFFE4E4E7)
                    )
                )
                OutlinedTextField(
                    value = sk,
                    onValueChange = { sk = it },
                    label = { Text(if (isEdit) "Secret Key (SK，留空保留原值)" else "Secret Key (SK)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF2563EB),
                        unfocusedBorderColor = Color(0xFFE4E4E7)
                    )
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank() && ak.isNotBlank()) onConfirm(type, name, ak, sk) },
                enabled = name.isNotBlank() && ak.isNotBlank()
            ) {
                Text(if (isEdit) "保存" else "创建", color = Color(0xFF2563EB))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = Color(0xFF71717A))
            }
        }
    )
}
