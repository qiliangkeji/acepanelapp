package com.acepanel.app.platform

import androidx.compose.runtime.Composable

interface ClipboardController {
    fun setText(text: String)
}

@Composable
expect fun rememberClipboardController(): ClipboardController
