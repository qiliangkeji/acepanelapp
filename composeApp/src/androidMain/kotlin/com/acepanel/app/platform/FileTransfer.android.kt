package com.acepanel.app.platform

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberFileTransferController(): FileTransferController {
    val context = LocalContext.current
    var pickCallback by remember { mutableStateOf<((LocalPickedFile?) -> Unit)?>(null) }
    var saveState by remember { mutableStateOf<SaveRequest?>(null) }
    val latestSaveState by rememberUpdatedState(saveState)

    val pickLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        val picked = uri?.let { context.readPickedFile(it) }
        pickCallback?.invoke(picked)
        pickCallback = null
    }

    val saveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        val request = latestSaveState
        if (uri == null || request == null) {
            request?.onSaved?.invoke(false, "未选择保存位置")
            saveState = null
            return@rememberLauncherForActivityResult
        }

        val result = runCatching {
            context.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(request.bytes)
                out.flush()
            } ?: error("无法打开保存位置")
        }
        request.onSaved(result.isSuccess, result.exceptionOrNull()?.message)
        saveState = null
    }

    return remember(context, pickLauncher, saveLauncher) {
        object : FileTransferController {
            override fun pickFile(onPicked: (LocalPickedFile?) -> Unit) {
                pickCallback = onPicked
                pickLauncher.launch("*/*")
            }

            override fun saveFile(fileName: String, bytes: ByteArray, onSaved: (Boolean, String?) -> Unit) {
                saveState = SaveRequest(bytes, onSaved)
                saveLauncher.launch(fileName)
            }
        }
    }
}

private data class SaveRequest(
    val bytes: ByteArray,
    val onSaved: (Boolean, String?) -> Unit
)

private fun Context.readPickedFile(uri: Uri): LocalPickedFile? {
    return runCatching {
        val name = resolveFileName(uri).ifBlank { "upload.bin" }
        val bytes = contentResolver.openInputStream(uri)?.use { input ->
            input.readBytes()
        } ?: return null
        LocalPickedFile(name, bytes)
    }.getOrNull()
}

private fun Context.resolveFileName(uri: Uri): String {
    contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0) {
                return cursor.getString(index).orEmpty()
            }
        }
    }
    return uri.lastPathSegment?.substringAfterLast('/') ?: ""
}
