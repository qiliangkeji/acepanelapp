package com.acepanel.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.acepanel.app.ui.screens.TerminalScreen
import kotlin.math.roundToInt

data class FloatingTerminalSession(
    val panelId: String,
    val sshId: Long = 0,
    val initialPath: String = "/",
    val minimized: Boolean = false
)

@Composable
fun FloatingTerminalHost(
    session: FloatingTerminalSession?,
    onMinimize: () -> Unit,
    onRestore: () -> Unit,
    onClose: () -> Unit
) {
    if (session == null) return

    BoxWithConstraints(modifier = Modifier.padding(12.dp)) {
        var dragX by remember(session.panelId, session.sshId, session.initialPath) { mutableFloatStateOf(0f) }
        var dragY by remember(session.panelId, session.sshId, session.initialPath) { mutableFloatStateOf(0f) }

        val windowWidth = if (maxWidth < 560.dp) maxWidth else 520.dp
        val windowHeight = if (maxHeight < 660.dp) maxHeight - 48.dp else 520.dp
        val dragModifier = Modifier.pointerInput(session.panelId, session.sshId, session.initialPath) {
            detectDragGestures { change, dragAmount ->
                change.consume()
                dragX += dragAmount.x
                dragY += dragAmount.y
            }
        }

        Box(
            modifier = if (session.minimized) {
                Modifier
                    .align(Alignment.BottomEnd)
                    .size(1.dp)
                    .graphicsLayer { alpha = 0f }
            } else {
                Modifier
                    .align(Alignment.BottomEnd)
                    .offset { IntOffset(dragX.roundToInt(), dragY.roundToInt()) }
                    .width(windowWidth)
                    .height(windowHeight)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF1A1A2E))
                    .border(1.dp, Color(0xFF3F4E6E), RoundedCornerShape(16.dp))
            }
        ) {
            TerminalScreen(
                panelId = session.panelId,
                sshId = session.sshId,
                initialPath = session.initialPath,
                showStatusBarSpacer = false,
                showBackButton = true,
                headerDragModifier = dragModifier,
                onBackClick = onClose,
                onMinimize = onMinimize
            )
        }

        if (session.minimized) {
            MinimizedTerminalPill(session = session, onRestore = onRestore, onClose = onClose)
        }
    }
}

@Composable
private fun MinimizedTerminalPill(
    session: FloatingTerminalSession,
    onRestore: () -> Unit,
    onClose: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color(0xF216213E))
            .border(1.dp, Color(0xFF3F4E6E), RoundedCornerShape(999.dp))
            .clickable(onClick = onRestore)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("终端", fontSize = 13.sp, color = Color(0xFFCDD6F4))
        Spacer(Modifier.width(8.dp))
        Text(session.initialPath.ifBlank { "/" }, fontSize = 11.sp, color = Color(0xFFA1A1AA), maxLines = 1)
        Spacer(Modifier.width(10.dp))
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Color(0xFFEF4444))
                .clickable(onClick = onClose),
            contentAlignment = Alignment.Center
        ) {
            Text("×", fontSize = 13.sp, color = Color.White)
        }
    }
}
