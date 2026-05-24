package com.acepanel.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import com.acepanel.app.feedback.FeedbackCenter
import com.acepanel.app.feedback.FeedbackMessage
import com.acepanel.app.feedback.FeedbackTone

@Composable
fun GlobalFeedbackHost(content: @Composable () -> Unit) {
    val state by FeedbackCenter.state.collectAsState()
    val activeOperation = state.activeOperations.lastOrNull()

    Box(modifier = Modifier.fillMaxSize()) {
        content()

        AnimatedVisibility(
            visible = activeOperation != null,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 54.dp, start = 16.dp, end = 16.dp),
            enter = slideInVertically(
                animationSpec = tween(220, easing = FastOutSlowInEasing),
                initialOffsetY = { -it }
            ) + fadeIn(tween(180)) + scaleIn(tween(180), initialScale = 0.96f),
            exit = slideOutVertically(
                animationSpec = tween(180, easing = FastOutSlowInEasing),
                targetOffsetY = { -it }
            ) + fadeOut(tween(140)) + scaleOut(tween(140), targetScale = 0.96f)
        ) {
            activeOperation?.let {
                ActiveOperationPill(
                    title = it.title,
                    detail = it.detail,
                    count = state.activeOperations.size
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            state.messages.forEach { message ->
                FeedbackToast(message = message)
            }
        }
    }
}

@Composable
private fun ActiveOperationPill(
    title: String,
    detail: String?,
    count: Int
) {
    val transition = rememberInfiniteTransition(label = "operation-pulse")
    val pulse by transition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(860, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "operation-pulse-alpha"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xF20F172A))
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            strokeWidth = 2.dp,
            color = Color(0xFF60A5FA),
            trackColor = Color.White.copy(alpha = 0.14f)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (count > 1) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "+${count - 1}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF93C5FD),
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color.White.copy(alpha = 0.12f))
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                }
            }
            if (!detail.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = detail,
                    fontSize = 12.sp,
                    color = Color(0xFFCBD5E1),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Canvas(modifier = Modifier.size(10.dp).graphicsLayer { alpha = pulse }) {
            drawCircle(color = Color(0xFF22C55E))
        }
    }
}

@Composable
private fun FeedbackToast(message: FeedbackMessage) {
    var visible by remember(message.id) { mutableStateOf(true) }

    LaunchedEffect(message.id) {
        delay(
            when (message.tone) {
                FeedbackTone.Error -> 5200L
                FeedbackTone.Warning -> 4600L
                else -> 3200L
            }
        )
        visible = false
        delay(180)
        FeedbackCenter.dismissMessage(message.id)
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            animationSpec = tween(220, easing = FastOutSlowInEasing),
            initialOffsetY = { it / 2 }
        ) + fadeIn(tween(180)) + scaleIn(tween(180), initialScale = 0.96f),
        exit = fadeOut(tween(140)) + scaleOut(tween(140), targetScale = 0.96f)
    ) {
        val palette = paletteFor(message.tone)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(palette.background)
                .border(1.dp, palette.border, RoundedCornerShape(14.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ToneMark(tone = message.tone, color = palette.accent)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = message.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = palette.title,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (!message.detail.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = message.detail,
                        fontSize = 12.sp,
                        color = palette.detail,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun ToneMark(tone: FeedbackTone, color: Color) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
    ) {
        when (tone) {
            FeedbackTone.Success -> Canvas(modifier = Modifier.size(15.dp)) {
                drawLine(
                    color = color,
                    start = androidx.compose.ui.geometry.Offset(size.width * 0.18f, size.height * 0.56f),
                    end = androidx.compose.ui.geometry.Offset(size.width * 0.42f, size.height * 0.78f),
                    strokeWidth = 2.4.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = color,
                    start = androidx.compose.ui.geometry.Offset(size.width * 0.42f, size.height * 0.78f),
                    end = androidx.compose.ui.geometry.Offset(size.width * 0.84f, size.height * 0.24f),
                    strokeWidth = 2.4.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
            FeedbackTone.Error -> Text("!", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = color)
            FeedbackTone.Warning -> Text("!", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = color)
            FeedbackTone.Info -> Canvas(modifier = Modifier.size(15.dp)) {
                drawCircle(
                    color = color,
                    style = Stroke(width = 2.dp.toPx())
                )
                drawCircle(
                    color = color,
                    radius = 1.4.dp.toPx(),
                    center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height * 0.32f)
                )
                drawLine(
                    color = color,
                    start = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height * 0.48f),
                    end = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height * 0.74f),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

private data class ToastPalette(
    val background: Color,
    val border: Color,
    val accent: Color,
    val title: Color,
    val detail: Color
)

private fun paletteFor(tone: FeedbackTone): ToastPalette {
    return when (tone) {
        FeedbackTone.Success -> ToastPalette(
            background = Color(0xFFF0FDF4),
            border = Color(0xFFBBF7D0),
            accent = Color(0xFF16A34A),
            title = Color(0xFF14532D),
            detail = Color(0xFF166534)
        )
        FeedbackTone.Error -> ToastPalette(
            background = Color(0xFFFEF2F2),
            border = Color(0xFFFECACA),
            accent = Color(0xFFDC2626),
            title = Color(0xFF7F1D1D),
            detail = Color(0xFF991B1B)
        )
        FeedbackTone.Warning -> ToastPalette(
            background = Color(0xFFFFFBEB),
            border = Color(0xFFFDE68A),
            accent = Color(0xFFD97706),
            title = Color(0xFF78350F),
            detail = Color(0xFF92400E)
        )
        FeedbackTone.Info -> ToastPalette(
            background = Color(0xFFEFF6FF),
            border = Color(0xFFBFDBFE),
            accent = Color(0xFF2563EB),
            title = Color(0xFF1E3A8A),
            detail = Color(0xFF1D4ED8)
        )
    }
}
