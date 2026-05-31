package com.acepanel.app.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.UIKit.UIPasteboard

@Composable
actual fun rememberClipboardController(): ClipboardController {
    return remember {
        object : ClipboardController {
            override fun setText(text: String) {
                UIPasteboard.generalPasteboard.string = text
            }
        }
    }
}
