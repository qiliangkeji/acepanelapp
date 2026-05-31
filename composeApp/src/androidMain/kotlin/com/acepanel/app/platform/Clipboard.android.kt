package com.acepanel.app.platform

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberClipboardController(): ClipboardController {
    val context = LocalContext.current
    return remember(context) {
        object : ClipboardController {
            override fun setText(text: String) {
                val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                manager.setPrimaryClip(ClipData.newPlainText("path", text))
            }
        }
    }
}
