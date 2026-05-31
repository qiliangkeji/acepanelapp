package com.acepanel.app.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberFileTransferController(): FileTransferController {
    return remember {
        object : FileTransferController {
            override fun pickFile(onPicked: (LocalPickedFile?) -> Unit) {
                onPicked(null)
            }

            override fun saveFile(fileName: String, bytes: ByteArray, onSaved: (Boolean, String?) -> Unit) {
                onSaved(false, "当前 iOS 客户端暂不支持本地文件选择和保存")
            }
        }
    }
}
