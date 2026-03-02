package org.example.project.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.example.project.data.BackupFileItem
import org.example.project.data.PanelRepository
import org.example.project.network.PanelApiService

class BackupViewModel : ViewModel() {

    private val _backups = MutableStateFlow<List<BackupFileItem>>(emptyList())
    val backups: StateFlow<List<BackupFileItem>> = _backups

    private val _backupType = MutableStateFlow("website")
    val backupType: StateFlow<String> = _backupType

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _actionFile = MutableStateFlow<String?>(null)
    val actionFile: StateFlow<String?> = _actionFile

    private val _actionError = MutableStateFlow<String?>(null)
    val actionError: StateFlow<String?> = _actionError

    private val _showCreateDialog = MutableStateFlow(false)
    val showCreateDialog: StateFlow<Boolean> = _showCreateDialog

    private val _isCreating = MutableStateFlow(false)
    val isCreating: StateFlow<Boolean> = _isCreating

    private var service: PanelApiService? = null

    fun init(panelId: String) {
        val cfg = PanelRepository.getPanel(panelId) ?: return
        service?.close()
        service = PanelApiService(cfg)
        viewModelScope.launch { load() }
    }

    fun loadByType(type: String) {
        _backupType.value = type
        viewModelScope.launch { load() }
    }

    fun load() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            service?.getBackups(_backupType.value, page = 1, limit = 100)
                ?.onSuccess { _backups.value = it.items }
                ?.onFailure { _error.value = it.message }
            _isLoading.value = false
        }
    }

    fun showCreate() { _actionError.value = null; _showCreateDialog.value = true }
    fun hideCreate() { _showCreateDialog.value = false }

    fun createBackup(target: String) {
        viewModelScope.launch {
            _isCreating.value = true
            _actionError.value = null
            service?.createBackup(_backupType.value, target)
                ?.onSuccess { _showCreateDialog.value = false; load() }
                ?.onFailure { _actionError.value = "备份失败: ${it.message}" }
            _isCreating.value = false
        }
    }

    private val _showRestoreDialog = MutableStateFlow<String?>(null) // 当前要恢复的文件名
    val showRestoreDialog: StateFlow<String?> = _showRestoreDialog

    private val _isRestoring = MutableStateFlow(false)
    val isRestoring: StateFlow<Boolean> = _isRestoring

    fun showRestore(fileName: String) { _actionError.value = null; _showRestoreDialog.value = fileName }
    fun hideRestore() { _showRestoreDialog.value = null }

    fun restoreBackup(file: String, target: String) {
        viewModelScope.launch {
            _isRestoring.value = true
            _actionError.value = null
            service?.restoreBackup(_backupType.value, file, target)
                ?.onSuccess { _showRestoreDialog.value = null }
                ?.onFailure { _actionError.value = "恢复失败: ${it.message}" }
            _isRestoring.value = false
        }
    }

    fun deleteBackup(fileName: String) {
        viewModelScope.launch {
            _actionFile.value = fileName
            _actionError.value = null
            service?.deleteBackup(_backupType.value, fileName)
                ?.onSuccess { _backups.value = _backups.value.filter { it.name != fileName } }
                ?.onFailure { _actionError.value = "删除失败: ${it.message}" }
            _actionFile.value = null
        }
    }

    override fun onCleared() {
        service?.close()
        super.onCleared()
    }
}
