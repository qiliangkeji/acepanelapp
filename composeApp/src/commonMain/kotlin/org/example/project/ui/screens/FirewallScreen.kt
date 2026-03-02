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
import androidx.compose.material3.OutlinedTextFieldDefaults
import org.example.project.data.FirewallRuleApiItem
import org.example.project.data.ForwardRuleApiItem
import org.example.project.data.IpRuleApiItem
import org.example.project.ui.components.StatusBarSpacer
import org.example.project.viewmodel.FirewallViewModel

@Composable
fun FirewallScreen(
    panelId: String = "",
    onBackClick: () -> Unit = {},
    onAddClick: () -> Unit = {}
) {
    val vm = viewModel<FirewallViewModel>()
    val firewallEnabled by vm.status.collectAsStateWithLifecycle()
    val rules by vm.rules.collectAsStateWithLifecycle()
    val total by vm.total.collectAsStateWithLifecycle()
    val ipRules by vm.ipRules.collectAsStateWithLifecycle()
    val forwardRules by vm.forwardRules.collectAsStateWithLifecycle()
    val isLoading by vm.isLoading.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    val actionError by vm.actionError.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableStateOf(0) } // 0=端口规则 1=IP规则 2=端口转发
    var showAddRuleDialog by remember { mutableStateOf(false) }
    var showAddIpDialog by remember { mutableStateOf(false) }
    var showAddForwardDialog by remember { mutableStateOf(false) }

    LaunchedEffect(panelId) {
        if (panelId.isNotEmpty()) vm.init(panelId)
    }

    if (showAddRuleDialog) {
        AddPortRuleDialog(
            actionError = actionError,
            onDismiss = { showAddRuleDialog = false },
            onAdd = { family, portStart, portEnd, protocol, address, strategy, direction ->
                vm.addRule(family, portStart, portEnd, protocol, address, strategy, direction)
                showAddRuleDialog = false
            }
        )
    }

    if (showAddIpDialog) {
        AddIpRuleDialog(
            actionError = actionError,
            onDismiss = { showAddIpDialog = false },
            onAdd = { family, protocol, address, strategy, direction ->
                vm.addIpRule(family, protocol, address, strategy, direction)
                showAddIpDialog = false
            }
        )
    }

    if (showAddForwardDialog) {
        AddForwardRuleDialog(
            actionError = actionError,
            onDismiss = { showAddForwardDialog = false },
            onAdd = { protocol, port, targetIp, targetPort ->
                vm.addForwardRule(protocol, port, targetIp, targetPort)
                showAddForwardDialog = false
            }
        )
    }

    Column(
        modifier = Modifier.fillMaxSize().background(Color.White)
    ) {
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
            ) { Text("‹", fontSize = 20.sp, color = Color(0xFF18181B)) }

            Text("防火墙", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF18181B))

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
                        .clickable {
                            when (selectedTab) {
                                0 -> showAddRuleDialog = true
                                1 -> showAddIpDialog = true
                                2 -> showAddForwardDialog = true
                                else -> onAddClick()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) { Text("+", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White) }
            }
        }

        // Firewall status card
        Box(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(16.dp)).border(1.5.dp, Color(0xFFE4E4E7), RoundedCornerShape(16.dp)).padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("防火墙状态", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF18181B))
                    Switch(
                        checked = firewallEnabled,
                        onCheckedChange = { vm.toggleStatus() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF2563EB),
                            uncheckedThumbColor = Color.White, uncheckedTrackColor = Color(0xFFE4E4E7)
                        )
                    )
                }
                Text(if (firewallEnabled) "已启用" else "已停用", fontSize = 13.sp, color = Color(0xFF71717A))
                if (error != null) Text(error!!, fontSize = 12.sp, color = Color(0xFFEF4444))
                if (actionError != null) Text(actionError!!, fontSize = 12.sp, color = Color(0xFFEF4444))
            }
        }

        // Tab bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("端口规则 ($total)", "IP 规则 (${ipRules.size})", "端口转发 (${forwardRules.size})").forEachIndexed { index, label ->
                Box(
                    modifier = Modifier.height(32.dp)
                        .background(if (selectedTab == index) Color(0xFF18181B) else Color(0xFFF3F4F6), RoundedCornerShape(8.dp))
                        .clickable { selectedTab = index }.padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(label, fontSize = 12.sp, fontWeight = if (selectedTab == index) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (selectedTab == index) Color.White else Color(0xFF71717A))
                }
            }
            if (isLoading) CircularProgressIndicator(modifier = Modifier.size(16.dp).align(Alignment.CenterVertically), strokeWidth = 2.dp, color = Color(0xFF2563EB))
        }

        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 24.dp, vertical = 4.dp)) {
            when (selectedTab) {
                0 -> {
                    if (rules.isEmpty() && !isLoading) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                                Text(if (panelId.isEmpty()) "请先选择面板" else "暂无端口规则", fontSize = 14.sp, color = Color(0xFFA1A1AA))
                            }
                        }
                    }
                    items(rules.size) { index ->
                        FirewallRuleCard(rule = rules[index], onDelete = { vm.deleteRule(rules[index]) })
                        Spacer(Modifier.height(10.dp))
                    }
                }
                1 -> {
                    if (ipRules.isEmpty() && !isLoading) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                                Text("暂无 IP 规则，点击 + 添加", fontSize = 14.sp, color = Color(0xFFA1A1AA))
                            }
                        }
                    }
                    items(ipRules.size) { index ->
                        IpRuleCard(rule = ipRules[index], onDelete = { vm.deleteIpRule(ipRules[index]) })
                        Spacer(Modifier.height(10.dp))
                    }
                }
                2 -> {
                    if (forwardRules.isEmpty() && !isLoading) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                                Text("暂无转发规则，点击 + 添加", fontSize = 14.sp, color = Color(0xFFA1A1AA))
                            }
                        }
                    }
                    items(forwardRules.size) { index ->
                        ForwardRuleCard(rule = forwardRules[index], onDelete = { vm.deleteForwardRule(forwardRules[index]) })
                        Spacer(Modifier.height(10.dp))
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun IpRuleCard(rule: IpRuleApiItem, onDelete: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .border(1.5.dp, Color(0xFFE4E4E7), RoundedCornerShape(16.dp)).padding(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(rule.address.ifEmpty { "全部" }, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF18181B))
                Text("${rule.direction.let { if (it == "in") "入站" else "出站" }} · ${rule.family} · ${rule.protocol.uppercase()}", fontSize = 12.sp, color = Color(0xFF71717A))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                val (bg, fg) = if (rule.strategy == "accept") Color(0xFFECFDF5) to Color(0xFF10B981) else Color(0xFFFEE2E2) to Color(0xFFEF4444)
                Box(modifier = Modifier.height(24.dp).clip(RoundedCornerShape(6.dp)).background(bg).padding(horizontal = 10.dp, vertical = 6.dp), contentAlignment = Alignment.Center) {
                    Text(if (rule.strategy == "accept") "放行" else "拒绝", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = fg)
                }
                Box(modifier = Modifier.size(24.dp).clip(RoundedCornerShape(6.dp)).background(Color(0xFFF3F4F6)).clickable(onClick = onDelete), contentAlignment = Alignment.Center) {
                    Text("×", fontSize = 14.sp, color = Color(0xFF71717A))
                }
            }
        }
    }
}

@Composable
private fun ForwardRuleCard(rule: ForwardRuleApiItem, onDelete: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .border(1.5.dp, Color(0xFFE4E4E7), RoundedCornerShape(16.dp)).padding(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("${rule.protocol.uppercase()} :${rule.port} → ${rule.target_ip}:${rule.target_port}",
                    fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF18181B))
                Text("端口转发规则", fontSize = 12.sp, color = Color(0xFF71717A))
            }
            Box(modifier = Modifier.size(24.dp).clip(RoundedCornerShape(6.dp)).background(Color(0xFFF3F4F6)).clickable(onClick = onDelete), contentAlignment = Alignment.Center) {
                Text("×", fontSize = 14.sp, color = Color(0xFF71717A))
            }
        }
    }
}

@Composable
private fun AddIpRuleDialog(
    actionError: String?,
    onDismiss: () -> Unit,
    onAdd: (String, String, String, String, String) -> Unit
) {
    var family by remember { mutableStateOf("ipv4") }
    var protocol by remember { mutableStateOf("tcp") }
    var address by remember { mutableStateOf("") }
    var strategy by remember { mutableStateOf("accept") }
    var direction by remember { mutableStateOf("in") }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("添加 IP 规则", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF18181B))

            OutlinedTextField(value = address, onValueChange = { address = it },
                label = { Text("IP 地址或 CIDR（必填）") }, modifier = Modifier.fillMaxWidth(), singleLine = true)

            OptionRow("IP 类型", listOf("ipv4" to "IPv4", "ipv6" to "IPv6"), family) { family = it }
            OptionRow("协议", listOf("tcp" to "TCP", "udp" to "UDP", "tcp/udp" to "TCP+UDP"), protocol) { protocol = it }
            OptionRow("方向", listOf("in" to "入站", "out" to "出站"), direction) { direction = it }
            OptionRow("策略", listOf("accept" to "放行", "drop" to "丢弃", "reject" to "拒绝"), strategy) { strategy = it }

            if (actionError != null) Text(actionError, fontSize = 12.sp, color = Color(0xFFEF4444))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("取消", color = Color(0xFF71717A)) }
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(8.dp))
                        .background(if (address.isNotBlank()) Color(0xFF2563EB) else Color(0xFF71717A))
                        .clickable(enabled = address.isNotBlank()) { onAdd(family, protocol, address.trim(), strategy, direction) }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) { Text("添加", fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Medium) }
            }
        }
    }
}

@Composable
private fun AddForwardRuleDialog(
    actionError: String?,
    onDismiss: () -> Unit,
    onAdd: (String, Int, String, Int) -> Unit
) {
    var protocol by remember { mutableStateOf("tcp") }
    var port by remember { mutableStateOf("") }
    var targetIp by remember { mutableStateOf("") }
    var targetPort by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("添加转发规则", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF18181B))

            OptionRow("协议", listOf("tcp" to "TCP", "udp" to "UDP"), protocol) { protocol = it }
            OutlinedTextField(value = port, onValueChange = { port = it }, label = { Text("本地端口") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = targetIp, onValueChange = { targetIp = it }, label = { Text("目标 IP") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = targetPort, onValueChange = { targetPort = it }, label = { Text("目标端口") }, modifier = Modifier.fillMaxWidth(), singleLine = true)

            if (actionError != null) Text(actionError, fontSize = 12.sp, color = Color(0xFFEF4444))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("取消", color = Color(0xFF71717A)) }
                Spacer(Modifier.width(8.dp))
                val portInt = port.toIntOrNull() ?: 0
                val tPortInt = targetPort.toIntOrNull() ?: 0
                val canSubmit = portInt > 0 && targetIp.isNotBlank() && tPortInt > 0
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(8.dp))
                        .background(if (canSubmit) Color(0xFF2563EB) else Color(0xFF71717A))
                        .clickable(enabled = canSubmit) { onAdd(protocol, portInt, targetIp.trim(), tPortInt) }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) { Text("添加", fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Medium) }
            }
        }
    }
}

@Composable
private fun OptionRow(label: String, options: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, fontSize = 12.sp, color = Color(0xFF71717A))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            options.forEach { (key, display) ->
                Box(
                    modifier = Modifier.height(30.dp)
                        .background(if (selected == key) Color(0xFF2563EB) else Color(0xFFF3F4F6), RoundedCornerShape(8.dp))
                        .clickable { onSelect(key) }.padding(horizontal = 10.dp),
                    contentAlignment = Alignment.Center
                ) { Text(display, fontSize = 12.sp, color = if (selected == key) Color.White else Color(0xFF71717A)) }
            }
        }
    }
}

@Composable
fun FirewallRuleCard(rule: FirewallRuleApiItem, onDelete: () -> Unit = {}) {
    Box(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .border(1.5.dp, Color(0xFFE4E4E7), RoundedCornerShape(16.dp)).padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                val portText = if (rule.port_start == rule.port_end) "${rule.protocol.uppercase()} ${rule.port_start}"
                else "${rule.protocol.uppercase()} ${rule.port_start}-${rule.port_end}"
                Text(portText, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF18181B))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    val (bg, fg) = if (rule.strategy == "accept") Color(0xFFECFDF5) to Color(0xFF10B981) else Color(0xFFFEE2E2) to Color(0xFFEF4444)
                    Box(modifier = Modifier.height(24.dp).clip(RoundedCornerShape(6.dp)).background(bg).padding(horizontal = 10.dp, vertical = 6.dp), contentAlignment = Alignment.Center) {
                        Text(if (rule.strategy == "accept") "放行" else "拒绝", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = fg)
                    }
                    Box(modifier = Modifier.size(24.dp).clip(RoundedCornerShape(6.dp)).background(Color(0xFFF3F4F6)).clickable(onClick = onDelete), contentAlignment = Alignment.Center) {
                        Text("×", fontSize = 14.sp, color = Color(0xFF71717A))
                    }
                }
            }
            Text(buildString {
                append(if (rule.direction == "in") "入站" else "出站")
                append(" · ${rule.family}")
                if (rule.address.isNotEmpty()) append(" · 来源: ${rule.address}")
                if (rule.in_use) append(" · 端口占用中")
            }, fontSize = 13.sp, color = Color(0xFF71717A))
        }
    }
}

@Composable
private fun AddPortRuleDialog(
    actionError: String?,
    onDismiss: () -> Unit,
    onAdd: (String, Int, Int, String, String, String, String) -> Unit
) {
    var family by remember { mutableStateOf("ipv4") }
    var portStart by remember { mutableStateOf("") }
    var portEnd by remember { mutableStateOf("") }
    var protocol by remember { mutableStateOf("tcp") }
    var address by remember { mutableStateOf("") }
    var strategy by remember { mutableStateOf("accept") }
    var direction by remember { mutableStateOf("in") }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("添加端口规则", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF18181B))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = portStart, onValueChange = { portStart = it },
                    label = { Text("起始端口") },
                    modifier = Modifier.weight(1f), singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF2563EB), unfocusedBorderColor = Color(0xFFE4E4E7)
                    )
                )
                OutlinedTextField(
                    value = portEnd, onValueChange = { portEnd = it },
                    label = { Text("结束端口") },
                    modifier = Modifier.weight(1f), singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF2563EB), unfocusedBorderColor = Color(0xFFE4E4E7)
                    )
                )
            }

            OutlinedTextField(
                value = address, onValueChange = { address = it },
                label = { Text("来源 IP（可选）") },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF2563EB), unfocusedBorderColor = Color(0xFFE4E4E7)
                )
            )

            OptionRow("IP 类型", listOf("ipv4" to "IPv4", "ipv6" to "IPv6"), family) { family = it }
            OptionRow("协议", listOf("tcp" to "TCP", "udp" to "UDP", "tcp/udp" to "TCP+UDP"), protocol) { protocol = it }
            OptionRow("方向", listOf("in" to "入站", "out" to "出站"), direction) { direction = it }
            OptionRow("策略", listOf("accept" to "放行", "drop" to "丢弃", "reject" to "拒绝"), strategy) { strategy = it }

            if (actionError != null) Text(actionError, fontSize = 12.sp, color = Color(0xFFEF4444))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("取消", color = Color(0xFF71717A)) }
                Spacer(Modifier.width(8.dp))
                val ps = portStart.toIntOrNull() ?: 0
                val pe = portEnd.toIntOrNull()?.takeIf { it >= ps } ?: ps
                val canSubmit = ps in 1..65535
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(8.dp))
                        .background(if (canSubmit) Color(0xFF2563EB) else Color(0xFF71717A))
                        .clickable(enabled = canSubmit) {
                            onAdd(family, ps, if (pe > 0) pe else ps, protocol, address.trim(), strategy, direction)
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) { Text("添加", fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Medium) }
            }
        }
    }
}
