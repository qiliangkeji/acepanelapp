package com.acepanel.app.platform

import androidx.compose.runtime.Composable

data class LocalPickedFile(
    val name: String,
    val bytes: ByteArray
)

interface FileTransferController {
    fun pickFile(onPicked: (LocalPickedFile?) -> Unit)
    fun saveFile(fileName: String, bytes: ByteArray, onSaved: (Boolean, String?) -> Unit)
}

@Composable
expect fun rememberFileTransferController(): FileTransferController
