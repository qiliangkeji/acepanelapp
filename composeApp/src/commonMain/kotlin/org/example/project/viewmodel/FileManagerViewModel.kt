package org.example.project.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.example.project.data.FileListItem
import org.example.project.data.PanelRepository
import org.example.project.network.PanelApiService

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

    // 路径历史栈（用于返回上级目录）
    private val pathStack = mutableListOf<String>()

    private var service: PanelApiService? = null

    fun init(panelId: String, initialPath: String = "/") {
        val cfg = PanelRepository.getPanel(panelId) ?: return
        service?.close()
        service = PanelApiService(cfg)
        pathStack.clear()
        _currentPath.value = initialPath
        viewModelScope.launch { load() }
    }

    fun load() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            service?.getFileList(_currentPath.value, page = 1, limit = 200)
                ?.onSuccess { _files.value = it.items }
                ?.onFailure { _error.value = it.message }
            _isLoading.value = false
        }
    }

    /** 进入子目录 */
    fun navigate(path: String) {
        pathStack.add(_currentPath.value)
        _currentPath.value = path
        viewModelScope.launch { load() }
    }

    /** 返回上级目录 */
    fun goBack(): Boolean {
        return if (pathStack.isNotEmpty()) {
            _currentPath.value = pathStack.removeLast()
            viewModelScope.launch { load() }
            true
        } else false
    }

    val canGoBack: Boolean get() = pathStack.isNotEmpty()

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

    fun showCreate() { _showCreateDialog.value = true }
    fun hideCreate() { _showCreateDialog.value = false }

    override fun onCleared() {
        service?.close()
        super.onCleared()
    }
}
