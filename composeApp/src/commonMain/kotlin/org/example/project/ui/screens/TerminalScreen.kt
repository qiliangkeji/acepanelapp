package org.example.project.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.example.project.ui.components.StatusBarSpacer
import org.example.project.viewmodel.TerminalViewModel

@Composable
fun TerminalScreen(
    panelId: String = "",
    sshId: Long = 0,
    onBackClick: () -> Unit = {}
) {
    val vm: TerminalViewModel = viewModel()
    val lines by vm.lines.collectAsStateWithLifecycle()
    val connState by vm.connState.collectAsStateWithLifecycle()
    val errorMessage by vm.errorMessage.collectAsStateWithLifecycle()

    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(panelId, sshId) {
        if (panelId.isNotEmpty()) vm.init(panelId, sshId)
    }

    // 有新行时自动滚动到底部
    LaunchedEffect(lines.size) {
        if (lines.isNotEmpty()) listState.animateScrollToItem(lines.lastIndex)
    }

    val title = if (sshId > 0) "SSH 终端 #$sshId" else "终端"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A2E))
    ) {
        StatusBarSpacer()

        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF16213E))
                .padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, Color(0xFF3F4E6E), RoundedCornerShape(8.dp))
                    .clickable(onClick = onBackClick),
                contentAlignment = Alignment.Center
            ) {
                Text("‹", fontSize = 20.sp, color = Color(0xFFCDD6F4))
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 连接状态指示灯
                val (dotColor, dotLabel) = when (connState) {
                    TerminalViewModel.ConnState.CONNECTED -> Color(0xFF4ADE80) to "已连接"
                    TerminalViewModel.ConnState.CONNECTING -> Color(0xFFFBBF24) to "连接中"
                    TerminalViewModel.ConnState.DISCONNECTED -> Color(0xFFA1A1AA) to "已断开"
                    TerminalViewModel.ConnState.ERROR -> Color(0xFFEF4444) to "错误"
                    else -> Color(0xFF3F4E6E) to "空闲"
                }
                if (connState == TerminalViewModel.ConnState.CONNECTING) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(10.dp),
                        strokeWidth = 1.5.dp,
                        color = Color(0xFFFBBF24)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(dotColor)
                    )
                }
                Text(
                    text = "$title · $dotLabel",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFCDD6F4)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // 清屏按钮
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFF3F4E6E), RoundedCornerShape(8.dp))
                        .clickable { vm.clear() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("⌫", fontSize = 14.sp, color = Color(0xFFA1A1AA))
                }
                // 重连按钮（断开/错误时显示）
                if (connState == TerminalViewModel.ConnState.DISCONNECTED ||
                    connState == TerminalViewModel.ConnState.ERROR
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF2563EB))
                            .clickable { vm.connect() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("↺", fontSize = 14.sp, color = Color.White)
                    }
                }
            }
        }

        // 错误信息显示
        if (connState == TerminalViewModel.ConnState.ERROR && errorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF2D1B1B))
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Text(
                    text = errorMessage!!,
                    fontSize = 13.sp,
                    color = Color(0xFFEF4444),
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // 终端输出区
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            itemsIndexed(lines) { _, line ->
                Text(
                    text = line,
                    fontSize = 13.sp,
                    color = Color(0xFFCDD6F4),
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 18.sp
                )
            }

            // 连接中占位
            if (connState == TerminalViewModel.ConnState.CONNECTING) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(12.dp),
                            strokeWidth = 1.5.dp,
                            color = Color(0xFFFBBF24)
                        )
                        Text(
                            text = "正在建立连接...",
                            fontSize = 13.sp,
                            color = Color(0xFFFBBF24),
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // 已断开提示
            if (connState == TerminalViewModel.ConnState.DISCONNECTED) {
                item {
                    Text(
                        text = "--- 连接已断开，点击右上角 ↺ 重连 ---",
                        fontSize = 12.sp,
                        color = Color(0xFF71717A),
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        // 快捷键栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF16213E))
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                "Ctrl+C" to { vm.sendCtrlC() },
                "Tab" to { vm.sendTab() },
                "↑" to { vm.sendInput("\u001B[A") },
                "↓" to { vm.sendInput("\u001B[B") },
                "←" to { vm.sendInput("\u001B[D") },
                "→" to { vm.sendInput("\u001B[C") }
            ).forEach { (label, action) ->
                Box(
                    modifier = Modifier
                        .height(28.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF252D3D))
                        .border(1.dp, Color(0xFF3F4E6E), RoundedCornerShape(6.dp))
                        .clickable(
                            enabled = connState == TerminalViewModel.ConnState.CONNECTED,
                            onClick = action
                        )
                        .padding(horizontal = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        color = if (connState == TerminalViewModel.ConnState.CONNECTED)
                            Color(0xFFCDD6F4) else Color(0xFF3F4E6E),
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // 输入栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0F3460))
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "▶",
                fontSize = 12.sp,
                color = Color(0xFF4ADE80),
                fontFamily = FontFamily.Monospace
            )
            BasicTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 4.dp),
                textStyle = TextStyle(
                    fontSize = 14.sp,
                    color = Color(0xFFCDD6F4),
                    fontFamily = FontFamily.Monospace
                ),
                cursorBrush = SolidColor(Color(0xFF4ADE80)),
                singleLine = true,
                decorationBox = { inner ->
                    Box {
                        if (inputText.isEmpty()) {
                            Text(
                                text = "输入命令...",
                                fontSize = 14.sp,
                                color = Color(0xFF3F4E6E),
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        inner()
                    }
                }
            )
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (connState == TerminalViewModel.ConnState.CONNECTED && inputText.isNotEmpty())
                            Color(0xFF2563EB) else Color(0xFF252D3D)
                    )
                    .clickable(
                        enabled = connState == TerminalViewModel.ConnState.CONNECTED && inputText.isNotEmpty()
                    ) {
                        vm.sendLine(inputText)
                        inputText = ""
                    },
                contentAlignment = Alignment.Center
            ) {
                Text("↵", fontSize = 16.sp, color = Color(0xFFCDD6F4))
            }
        }
    }
}
