package com.acepanel.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.acepanel.app.data.PanelConfig
import com.acepanel.app.data.PanelRuntimeStatus
import com.acepanel.app.feedback.FeedbackCenter
import com.acepanel.app.ui.components.AnimatedAppDialog
import com.acepanel.app.ui.components.AppIcon
import com.acepanel.app.ui.components.MenuSvgIcon
import com.acepanel.app.ui.components.OnlineBadge
import com.acepanel.app.ui.components.PanelCard
import com.acepanel.app.ui.components.StatusBarSpacer
import com.acepanel.app.viewmodel.HomeViewModel

private enum class HomeTab(
    val title: String,
    val subtitle: String,
    val label: String
) {
    Panels("我的面板", "管理您的所有服务器面板", "面板"),
    Terminal("终端", "服务器远程命令入口", "终端"),
    Alert("预警", "服务器告警和异常通知", "预警"),
    About("关于", "客户端与面板信息", "关于")
}

private val homeTabs = listOf(HomeTab.Panels, HomeTab.Terminal, HomeTab.Alert, HomeTab.About)

@Composable
fun HomeScreen(
    onPanelClick: (String) -> Unit = {},
    onAddClick: () -> Unit = {},
    onPanelEdit: (String) -> Unit = {}
) {
    val vm: HomeViewModel = viewModel()
    val panels by vm.panels.collectAsStateWithLifecycle()
    val statuses by vm.statuses.collectAsStateWithLifecycle()
    val isRefreshing by vm.isRefreshing.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableStateOf(HomeTab.Panels) }
    var actionPanel by remember { mutableStateOf<PanelConfig?>(null) }
    var deletePanel by remember { mutableStateOf<PanelConfig?>(null) }

    // 进入页面时刷新状态
    LaunchedEffect(Unit) {
        vm.loadPanels()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        StatusBarSpacer()
        HomeHeader(
            selectedTab = selectedTab,
            isRefreshing = isRefreshing,
            onAddClick = onAddClick
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            when (selectedTab) {
                HomeTab.Panels -> PanelHomeContent(
                    panels = panels,
                    statuses = statuses,
                    onPanelClick = onPanelClick,
                    onPanelLongClick = { actionPanel = it }
                )
                HomeTab.About -> AboutHomeContent()
                HomeTab.Terminal,
                HomeTab.Alert -> DevelopingHomeContent(tab = selectedTab)
            }
        }

        HomeBottomBar(
            selectedTab = selectedTab,
            onSelected = { selectedTab = it }
        )
    }

    actionPanel?.let { panel ->
        PanelActionDialog(
            panel = panel,
            status = statuses[panel.id] ?: PanelRuntimeStatus(),
            onDismiss = { actionPanel = null },
            onOpen = {
                actionPanel = null
                onPanelClick(panel.id)
            },
            onEdit = {
                actionPanel = null
                onPanelEdit(panel.id)
            },
            onDelete = {
                actionPanel = null
                deletePanel = panel
            }
        )
    }

    deletePanel?.let { panel ->
        DeletePanelConfirmDialog(
            panel = panel,
            onDismiss = { deletePanel = null },
            onConfirm = {
                vm.removePanel(panel.id)
                deletePanel = null
                FeedbackCenter.success("面板已删除", panel.name)
            }
        )
    }
}

@Composable
private fun HomeHeader(
    selectedTab: HomeTab,
    isRefreshing: Boolean,
    onAddClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = selectedTab.title,
                fontSize = 28.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF18181B),
                letterSpacing = (-0.5).sp
            )
            if (selectedTab == HomeTab.Panels) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isRefreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = Color(0xFF2563EB)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2563EB))
                            .clickable(onClick = onAddClick),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "+",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        Text(
            text = selectedTab.subtitle,
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            color = Color(0xFF71717A)
        )
    }
}

@Composable
private fun PanelHomeContent(
    panels: List<PanelConfig>,
    statuses: Map<String, PanelRuntimeStatus>,
    onPanelClick: (String) -> Unit,
    onPanelLongClick: (PanelConfig) -> Unit
) {
    if (panels.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "还没有面板",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF71717A)
                )
                Text(
                    text = "点击右上角 + 添加您的第一个面板",
                    fontSize = 14.sp,
                    color = Color(0xFFA1A1AA)
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(panels) { panel ->
                val status = statuses[panel.id] ?: PanelRuntimeStatus()
                PanelCard(
                    config = panel,
                    status = status,
                    onClick = { onPanelClick(panel.id) },
                    onLongClick = { onPanelLongClick(panel) }
                )
            }
        }
    }
}

@Composable
private fun PanelActionDialog(
    panel: PanelConfig,
    status: PanelRuntimeStatus,
    onDismiss: () -> Unit,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    AnimatedAppDialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Color.White)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = panel.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF18181B)
                    )
                    Text(
                        text = "${panel.scheme}://${panel.host}:${panel.port}",
                        fontSize = 13.sp,
                        color = Color(0xFF71717A)
                    )
                }
                OnlineBadge(isOnline = status.isOnline)
            }

            HorizontalDivider(thickness = 1.dp, color = Color(0xFFE4E4E7))

            PanelActionRow(
                icon = AppIcon.Monitor,
                title = "进入面板",
                subtitle = "打开该服务器的功能菜单",
                onClick = onOpen
            )
            PanelActionRow(
                icon = AppIcon.Settings,
                title = "编辑面板",
                subtitle = "修改名称、地址、入口和登录方式",
                onClick = onEdit
            )
            PanelActionRow(
                icon = AppIcon.Delete,
                title = "删除面板",
                subtitle = "仅删除客户端本地配置",
                danger = true,
                onClick = onDelete
            )
        }
    }
}

@Composable
private fun PanelActionRow(
    icon: AppIcon,
    title: String,
    subtitle: String,
    danger: Boolean = false,
    onClick: () -> Unit
) {
    val color = if (danger) Color(0xFFEF4444) else Color(0xFF2563EB)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (danger) Color(0xFFFEF2F2) else Color(0xFFF9FAFB))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MenuSvgIcon(icon = icon, color = color)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (danger) Color(0xFFDC2626) else Color(0xFF18181B)
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = if (danger) Color(0xFFB91C1C) else Color(0xFF71717A)
                )
            }
        }
        Text(text = "›", fontSize = 22.sp, color = if (danger) Color(0xFFF87171) else Color(0xFFA1A1AA))
    }
}

@Composable
private fun DeletePanelConfirmDialog(
    panel: PanelConfig,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AnimatedAppDialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Color.White)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "删除面板",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF18181B)
            )
            Text(
                text = "确定删除「${panel.name}」吗？这只会移除客户端保存的连接配置，不会影响服务器上的面板。",
                fontSize = 14.sp,
                color = Color(0xFF71717A)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .height(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onDismiss)
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("取消", fontSize = 14.sp, color = Color(0xFF71717A), fontWeight = FontWeight.Medium)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .height(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFEF4444))
                        .clickable(onClick = onConfirm)
                        .padding(horizontal = 18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("删除", fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun AboutHomeContent() {
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .padding(top = 12.dp)
                .width(132.dp)
                .height(36.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFF2563EB))
                .clickable {
                    uriHandler.openUri("https://github.com/qiliangkeji/acepanelapp")
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "开源地址",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFFEFF6FF)),
                contentAlignment = Alignment.Center
            ) {
                HomeBottomIcon(tab = HomeTab.About, color = Color(0xFF2563EB), modifier = Modifier.size(26.dp))
            }
            Text(
                text = "AcePanel App",
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF18181B)
            )
            Text(
                text = "服务器面板移动客户端",
                fontSize = 14.sp,
                color = Color(0xFF71717A),
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.weight(1.4f))
    }
}

@Composable
private fun DevelopingHomeContent(tab: HomeTab) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFFEFF6FF)),
                contentAlignment = Alignment.Center
            ) {
                HomeBottomIcon(tab = tab, color = Color(0xFF2563EB), modifier = Modifier.size(26.dp))
            }
            Text(
                text = "开发中",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF18181B)
            )
            Text(
                text = "${tab.title}页面正在开发中",
                fontSize = 14.sp,
                color = Color(0xFF71717A),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun HomeBottomBar(
    selectedTab: HomeTab,
    onSelected: (HomeTab) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 18.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(76.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(Color(0xFFF3F4F6))
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            homeTabs.forEach { tab ->
                HomeBottomBarItem(
                    tab = tab,
                    selected = tab == selectedTab,
                    onClick = { onSelected(tab) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun HomeBottomBarItem(
    tab: HomeTab,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val foreground = if (selected) Color(0xFF374151) else Color(0xFF9CA3AF)
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(22.dp))
            .background(if (selected) Color.White else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(if (selected) Color(0xFFE5E7EB) else Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            HomeBottomIcon(
                tab = tab,
                color = foreground,
                modifier = Modifier.size(if (selected) 20.dp else 22.dp)
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = tab.label,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = foreground
        )
    }
}

@Composable
private fun HomeBottomIcon(
    tab: HomeTab,
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = 2.dp.toPx()

        when (tab) {
            HomeTab.Panels -> {
                val tile = w * 0.32f
                val gap = w * 0.14f
                val left = (w - tile * 2 - gap) / 2f
                val top = (h - tile * 2 - gap) / 2f
                listOf(0f, tile + gap).forEach { dx ->
                    listOf(0f, tile + gap).forEach { dy ->
                        drawRoundRect(
                            color = color,
                            topLeft = Offset(left + dx, top + dy),
                            size = Size(tile, tile),
                            cornerRadius = CornerRadius(3.dp.toPx()),
                            style = Stroke(width = stroke)
                        )
                    }
                }
            }
            HomeTab.Terminal -> {
                drawLine(color, Offset(w * 0.20f, h * 0.30f), Offset(w * 0.43f, h * 0.50f), strokeWidth = stroke)
                drawLine(color, Offset(w * 0.43f, h * 0.50f), Offset(w * 0.20f, h * 0.70f), strokeWidth = stroke)
                drawLine(color, Offset(w * 0.52f, h * 0.70f), Offset(w * 0.82f, h * 0.70f), strokeWidth = stroke)
            }
            HomeTab.Alert -> {
                val path = Path().apply {
                    moveTo(w * 0.50f, h * 0.16f)
                    lineTo(w * 0.86f, h * 0.82f)
                    lineTo(w * 0.14f, h * 0.82f)
                    close()
                }
                drawPath(path = path, color = color, style = Stroke(width = stroke))
                drawLine(color, Offset(w * 0.50f, h * 0.38f), Offset(w * 0.50f, h * 0.58f), strokeWidth = stroke)
                drawCircle(color = color, radius = 1.6.dp.toPx(), center = Offset(w * 0.50f, h * 0.70f))
            }
            HomeTab.About -> {
                drawCircle(color = color, radius = minOf(w, h) * 0.38f, center = Offset(w / 2f, h / 2f), style = Stroke(width = stroke))
                drawCircle(color = color, radius = 1.5.dp.toPx(), center = Offset(w / 2f, h * 0.34f))
                drawLine(color, Offset(w / 2f, h * 0.48f), Offset(w / 2f, h * 0.70f), strokeWidth = stroke)
            }
        }
    }
}
