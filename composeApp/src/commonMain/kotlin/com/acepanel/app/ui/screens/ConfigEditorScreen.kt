package com.acepanel.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
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
import com.acepanel.app.ui.components.BackButton
import com.acepanel.app.ui.components.StatusBarSpacer
import com.acepanel.app.viewmodel.ConfigEditorViewModel

@Composable
fun ConfigEditorScreen(
    panelId: String = "",
    filePath: String = "",
    fileName: String = "配置编辑器",
    serviceName: String = "",
    onBack: () -> Unit = {},
    onSaveClick: () -> Unit = {},
    showStatusBarSpacer: Boolean = true,
    showBackButton: Boolean = true
) {
    val vm: ConfigEditorViewModel = viewModel()
    val content by vm.content.collectAsStateWithLifecycle()
    val isLoading by vm.isLoading.collectAsStateWithLifecycle()
    val isSaving by vm.isSaving.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    val saveResult by vm.saveResult.collectAsStateWithLifecycle()
    val editorScrollState = rememberScrollState()
    val isLogView = serviceName.isNotBlank() || filePath.substringAfterLast('/').endsWith(".log", ignoreCase = true)
    val editorTextStyle = TextStyle(
        color = Color(0xFFE4E4E7),
        fontFamily = FontFamily.Monospace,
        fontSize = 13.sp,
        lineHeight = 20.sp
    )

    LaunchedEffect(panelId, filePath, serviceName) {
        if (panelId.isNotEmpty()) vm.init(panelId, filePath, serviceName)
    }

    LaunchedEffect(content, isLoading, isLogView) {
        if (!isLoading && isLogView) {
            withFrameNanos { }
            editorScrollState.scrollTo(editorScrollState.maxValue)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1E1E1E))
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
                BackButton(onClick = onBack, isDarkTheme = true)
            } else {
                Spacer(modifier = Modifier.size(36.dp))
            }

            Text(
                text = if (filePath.isNotEmpty()) filePath.substringAfterLast('/') else fileName,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )

            Box(
                modifier = Modifier
                    .height(32.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (filePath.isNotEmpty() && !isLogView && !isSaving) Color(0xFF2563EB)
                        else Color(0xFF3F3F46)
                    )
                    .clickable(enabled = filePath.isNotEmpty() && !isLogView && !isSaving) {
                        vm.save(onSaveClick)
                    }
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                } else {
                    Text(
                        text = "保存",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }
        }

        // 错误提示
        if (error != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF3F1515))
                    .padding(8.dp)
            ) {
                Text(text = "⚠ $error", fontSize = 12.sp, color = Color(0xFFEF4444))
            }
        }

        // 保存结果提示
        if (saveResult != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (saveResult!!.startsWith("保存成功")) Color(0xFF14312A) else Color(0xFF3F1515))
                    .padding(8.dp)
            ) {
                Text(
                    text = saveResult!!,
                    fontSize = 12.sp,
                    color = if (saveResult!!.startsWith("保存成功")) Color(0xFF10B981) else Color(0xFFEF4444)
                )
            }
        }

        when {
            filePath.isEmpty() && serviceName.isEmpty() -> {
                // 无文件路径时的占位提示
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(text = "📁", fontSize = 40.sp)
                        Text(
                            text = "请从文件管理器打开文件",
                            fontSize = 15.sp,
                            color = Color(0xFF71717A)
                        )
                        Text(
                            text = "在文件管理器中点击文件即可编辑",
                            fontSize = 13.sp,
                            color = Color(0xFF52525B)
                        )
                    }
                }
            }
            isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        strokeWidth = 2.dp,
                        color = Color(0xFF2563EB)
                    )
                }
            }
            else -> {
                // 路径提示
                Text(
                    text = filePath.ifBlank { "systemd: $serviceName" },
                    fontSize = 11.sp,
                    color = Color(0xFF52525B),
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )
                // 编辑器区域
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 4.dp, vertical = 4.dp)
                ) {
                    if (isLogView) {
                        SelectionContainer {
                            Text(
                                text = content,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp)
                                    .verticalScroll(editorScrollState),
                                style = editorTextStyle
                            )
                        }
                    } else {
                        BasicTextField(
                            value = content,
                            onValueChange = { vm.content.value = it },
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                                .verticalScroll(editorScrollState),
                            textStyle = editorTextStyle,
                            cursorBrush = SolidColor(Color(0xFF2563EB))
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CodeLine(text: String, color: Color) {
    Text(
        text = text,
        fontSize = 13.sp,
        fontWeight = FontWeight.Normal,
        color = color,
        fontFamily = FontFamily.Monospace
    )
}
