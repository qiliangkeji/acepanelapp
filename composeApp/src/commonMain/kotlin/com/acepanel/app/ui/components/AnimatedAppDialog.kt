package com.acepanel.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun AnimatedAppDialog(
    onDismissRequest: () -> Unit,
    properties: DialogProperties = DialogProperties(),
    content: @Composable () -> Unit
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = properties
    ) {
        var visible by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            visible = true
        }

        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(150, easing = FastOutSlowInEasing)) +
                scaleIn(
                    animationSpec = tween(210, easing = FastOutSlowInEasing),
                    initialScale = 0.94f
                ) +
                slideInVertically(
                    animationSpec = tween(210, easing = FastOutSlowInEasing),
                    initialOffsetY = { it / 10 }
                ),
            exit = fadeOut(tween(120, easing = FastOutSlowInEasing)) +
                scaleOut(
                    animationSpec = tween(140, easing = FastOutSlowInEasing),
                    targetScale = 0.96f
                ) +
                slideOutVertically(
                    animationSpec = tween(140, easing = FastOutSlowInEasing),
                    targetOffsetY = { it / 12 }
                )
        ) {
            content()
        }
    }
}
