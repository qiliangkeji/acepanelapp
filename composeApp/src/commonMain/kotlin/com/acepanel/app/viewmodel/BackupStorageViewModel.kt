package com.acepanel.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.acepanel.app.data.BackupStorageInfo
import com.acepanel.app.data.BackupStorageItem
import com.acepanel.app.data.CreateBackupStorageRequest
import com.acepanel.app.data.PanelRepository
import com.acepanel.app.network.PanelApiService

class BackupStorageViewModel : ViewModel() {

    private val _storages = MutableStateFlow<List<BackupStorageItem>>(emptyList())
    val storages: StateFlow<List<BackupStorageItem>> = _storages

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _actionId = MutableStateFlow<Long?>(null)
    val actionId: StateFlow<Long?> = _actionId

    private val _actionError = MutableStateFlow<String?>(null)
    val actionError: StateFlow<String?> = _actionError

    private val _showCreateDialog = MutableStateFlow(false)
    val showCreateDialog: StateFlow<Boolean> = _showCreateDialog

    private val _editStorage = MutableStateFlow<BackupStorageItem?>(null)
    val editStorage: StateFlow<BackupStorageItem?> = _editStorage

    private var service: PanelApiService? = null

    fun init(panelId: String) {
        val cfg = PanelRepository.getPanel(panelId) ?: return
        service?.close()
        service = PanelApiService(cfg)
        viewModelScope.launch { load() }
    }

    fun load() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            service?.getBackupStorages(limit = 50)
                ?.onSuccess { _storages.value = it.items }
                ?.onFailure { _error.value = it.message }
            _isLoading.value = false
        }
    }

    fun showCreate() { _showCreateDialog.value = true; _actionError.value = null }
    fun hideCreate() { _showCreateDialog.value = false }
    fun showEdit(storage: BackupStorageItem) { _editStorage.value = storage; _actionError.value = null }
    fun hideEdit() { _editStorage.value = null; _actionError.value = null }

    fun createStorage(type: String, name: String, info: BackupStorageInfo) {
        viewModelScope.launch {
            _actionError.value = null
            service?.createBackupStorage(CreateBackupStorageRequest(type, name, info))
                ?.onSuccess {
                    hideCreate()
                    load()
                }
                ?.onFailure { _actionError.value = it.message }
        }
    }

    fun updateStorage(id: Long, type: String, name: String, info: BackupStorageInfo) {
        viewModelScope.launch {
            _actionError.value = null
            service?.updateBackupStorage(id, CreateBackupStorageRequest(type, name, info))
                ?.onSuccess {
                    hideEdit()
                    load()
                }
                ?.onFailure { _actionError.value = it.message }
        }
    }

    fun deleteStorage(id: Long) {
        viewModelScope.launch {
            _actionId.value = id
            _actionError.value = null
            service?.deleteBackupStorage(id)
                ?.onSuccess { _storages.value = _storages.value.filter { it.id != id } }
                ?.onFailure { _actionError.value = "删除失败: ${it.message}" }
            _actionId.value = null
        }
    }

    override fun onCleared() {
        service?.close()
        super.onCleared()
    }
}
