package com.acepanel.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.acepanel.app.data.FileControlRequest
import com.acepanel.app.data.FileListItem
import com.acepanel.app.data.PanelRepository
import com.acepanel.app.network.PanelApiService

class FileManagerViewModel : ViewModel() {

    private val _files = MutableStateFlow<List<FileListItem>>(emptyList())
    val files: StateFlow<List<FileListItem>> = _files

    private val _currentPath = MutableStateFlow("/")
    val currentPath: StateFlow<String> = _currentPath

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _actionError = MutableStateFlow<String?>(null)
    val actionError: StateFlow<String?> = _actionError

    private val _showCreateDialog = MutableStateFlow(false)
    val showCreateDialog: StateFlow<Boolean> = _showCreateDialog

    private val _clipboard = MutableStateFlow<FileClipboard?>(null)
    val clipboard: StateFlow<FileClipboard?> = _clipboard

    private var service: PanelApiService? = null
    private var currentPanelId: String? = null

    fun init(panelId: String, initialPath: String = "/") {
        val normalizedPath = normalizePath(initialPath)
        if (panelId == currentPanelId && service != null && _currentPath.value == normalizedPath) return

        val cfg = PanelRepository.getPanel(panelId) ?: return
        currentPanelId = panelId
        service?.close()
        service = PanelApiService(cfg)
        _currentPath.value = normalizedPath
        _files.value = emptyList()
        load()
    }

    fun load() {
        viewModelScope.launch {
            val requestedPath = _currentPath.value
            _isLoading.value = true
            _error.value = null
            service?.getFileList(requestedPath, page = 1, limit = 200)
                ?.onSuccess {
                    if (_currentPath.value == requestedPath) {
                        _files.value = it.items
                    }
                }
                ?.onFailure {
                    if (_currentPath.value == requestedPath) {
                        _error.value = it.message
                    }
                }
            if (_currentPath.value == requestedPath) {
                _isLoading.value = false
            }
        }
    }

    private fun normalizePath(path: String): String {
        val trimmed = path.trim().ifBlank { "/" }
        return if (trimmed.startsWith("/")) trimmed else "/$trimmed"
    }

    fun deleteFile(fullPath: String) {
        viewModelScope.launch {
            _actionError.value = null
            service?.deleteFile(fullPath)
                ?.onSuccess { load() }
                ?.onFailure { _actionError.value = "删除失败: ${it.message}" }
        }
    }

    fun createItem(name: String, isDir: Boolean) {
        val newPath = if (_currentPath.value.endsWith("/"))
            "${_currentPath.value}$name"
        else
            "${_currentPath.value}/$name"
        viewModelScope.launch {
            _actionError.value = null
            service?.createFile(newPath, isDir)
                ?.onSuccess { load() }
                ?.onFailure { _actionError.value = "创建失败: ${it.message}" }
        }
    }

    fun renameFile(file: FileListItem, newName: String) {
        val targetName = newName.trim()
        if (targetName.isEmpty() || targetName == file.name) return
        val parent = file.full.substringBeforeLast('/', missingDelimiterValue = "/").ifEmpty { "/" }
        val target = joinPath(parent, targetName)
        viewModelScope.launch {
            _actionError.value = null
            service?.moveFiles(listOf(FileControlRequest(file.full, target, force = false)))
                ?.onSuccess { load() }
                ?.onFailure { _actionError.value = "重命名失败: ${it.message}" }
        }
    }

    fun markForCopy(file: FileListItem) {
        _clipboard.value = FileClipboard(FileClipboardMode.Copy, file.full, file.name)
        _actionError.value = null
    }

    fun markForMove(file: FileListItem) {
        _clipboard.value = FileClipboard(FileClipboardMode.Move, file.full, file.name)
        _actionError.value = null
    }

    fun clearClipboard() {
        _clipboard.value = null
    }

    fun pasteMarked() {
        val clip = _clipboard.value ?: return
        val target = joinPath(_currentPath.value, clip.name)
        viewModelScope.launch {
            _actionError.value = null
            val req = listOf(FileControlRequest(clip.source, target, force = false))
            val result = when (clip.mode) {
                FileClipboardMode.Copy -> service?.copyFiles(req)
                FileClipboardMode.Move -> service?.moveFiles(req)
            }
            result?.onSuccess {
                if (clip.mode == FileClipboardMode.Move) _clipboard.value = null
                load()
            }?.onFailure { _actionError.value = "${if (clip.mode == FileClipboardMode.Copy) "复制" else "移动"}失败: ${it.message}" }
        }
    }

    fun updatePermission(file: FileListItem, mode: String, owner: String, group: String) {
        viewModelScope.launch {
            _actionError.value = null
            service?.updateFilePermission(file.full, normalizeMode(mode), owner.trim(), group.trim())
                ?.onSuccess { load() }
                ?.onFailure { _actionError.value = "修改权限失败: ${it.message}" }
        }
    }

    fun compress(file: FileListItem, archiveName: String) {
        val output = archiveName.trim()
        if (output.isEmpty()) return
        val archivePath = joinPath(_currentPath.value, output)
        viewModelScope.launch {
            _actionError.value = null
            service?.compressFiles(_currentPath.value, listOf(file.full), archivePath)
                ?.onSuccess { load() }
                ?.onFailure { _actionError.value = "压缩失败: ${it.message}" }
        }
    }

    fun unCompress(file: FileListItem, targetPath: String) {
        val target = normalizePath(targetPath.ifBlank { _currentPath.value })
        viewModelScope.launch {
            _actionError.value = null
            service?.unCompressFile(file.full, target)
                ?.onSuccess { load() }
                ?.onFailure { _actionError.value = "解压失败: ${it.message}" }
        }
    }

    fun remoteDownload(url: String, fileName: String) {
        val trimmedUrl = url.trim()
        val trimmedName = fileName.trim()
        if (trimmedUrl.isEmpty() || trimmedName.isEmpty()) return
        val target = joinPath(_currentPath.value, trimmedName)
        viewModelScope.launch {
            _actionError.value = null
            service?.remoteDownloadFile(target, trimmedUrl)
                ?.onSuccess { load() }
                ?.onFailure { _actionError.value = "远程下载失败: ${it.message}" }
        }
    }

    fun uploadLocalFile(name: String, bytes: ByteArray, force: Boolean = false) {
        val target = joinPath(_currentPath.value, name.trim())
        viewModelScope.launch {
            _actionError.value = null
            service?.uploadFile(target, name.trim(), bytes, force)
                ?.onSuccess { load() }
                ?.onFailure { _actionError.value = "上传失败: ${it.message}" }
        }
    }

    fun downloadFile(file: FileListItem, onResult: (ByteArray?, String?) -> Unit) {
        if (file.dir) {
            onResult(null, "目录请先压缩后下载")
            return
        }
        viewModelScope.launch {
            _actionError.value = null
            val result = service?.downloadFile(file.full)
            result
                ?.onSuccess { onResult(it, null) }
                ?.onFailure {
                    _actionError.value = "下载失败: ${it.message}"
                    onResult(null, it.message)
                }
        }
    }

    fun showCreate() { _showCreateDialog.value = true }
    fun hideCreate() { _showCreateDialog.value = false }

    fun setActionError(message: String?) {
        _actionError.value = message
    }

    private fun joinPath(dir: String, name: String): String {
        val normalizedDir = normalizePath(dir)
        return if (normalizedDir == "/") "/$name" else "$normalizedDir/$name"
    }

    private fun normalizeMode(mode: String): String {
        val digits = mode.filter { it in '0'..'7' }.takeLast(4)
        return when (digits.length) {
            0 -> "0755"
            3 -> "0$digits"
            else -> digits.padStart(4, '0')
        }
    }

    override fun onCleared() {
        service?.close()
        super.onCleared()
    }
}

enum class FileClipboardMode { Copy, Move }

data class FileClipboard(
    val mode: FileClipboardMode,
    val source: String,
    val name: String
)
