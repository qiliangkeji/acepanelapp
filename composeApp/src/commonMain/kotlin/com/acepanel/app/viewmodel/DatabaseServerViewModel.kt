package com.acepanel.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.acepanel.app.data.CreateDatabaseServerRequest
import com.acepanel.app.data.DatabaseServerApiItem
import com.acepanel.app.data.PanelRepository
import com.acepanel.app.data.UpdateDatabaseServerRequest
import com.acepanel.app.network.PanelApiService

class DatabaseServerViewModel : ViewModel() {

    private val _servers = MutableStateFlow<List<DatabaseServerApiItem>>(emptyList())
    val servers: StateFlow<List<DatabaseServerApiItem>> = _servers

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

    private val _showEditDialog = MutableStateFlow<DatabaseServerApiItem?>(null)
    val showEditDialog: StateFlow<DatabaseServerApiItem?> = _showEditDialog

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
            service?.getDatabaseServers(limit = 50)
                ?.onSuccess { _servers.value = it.items }
                ?.onFailure { _error.value = it.message }
            _isLoading.value = false
        }
    }

    fun showCreate() { _showCreateDialog.value = true }
    fun hideCreate() { _showCreateDialog.value = false }
    fun showEdit(server: DatabaseServerApiItem) { _showEditDialog.value = server; _actionError.value = null }
    fun hideEdit() { _showEditDialog.value = null }

    fun createServer(name: String, type: String, host: String, port: Int, username: String, password: String, remark: String) {
        viewModelScope.launch {
            _actionError.value = null
            service?.createDatabaseServer(
                CreateDatabaseServerRequest(name, type, host, port, username, password, remark)
            )?.onSuccess {
                hideCreate()
                load()
            }?.onFailure { _actionError.value = it.message }
        }
    }

    fun updateServer(id: Long, name: String, host: String, port: Int, username: String, password: String, remark: String) {
        viewModelScope.launch {
            _actionId.value = id
            _actionError.value = null
            service?.updateDatabaseServer(
                id,
                UpdateDatabaseServerRequest(name, host, port, username, password, remark)
            )?.onSuccess {
                _servers.value = _servers.value.map {
                    if (it.id == id) it.copy(name = name, host = host, port = port, remark = remark) else it
                }
                _showEditDialog.value = null
            }?.onFailure { _actionError.value = "更新失败: ${it.message}" }
            _actionId.value = null
        }
    }

    fun deleteServer(id: Long) {
        viewModelScope.launch {
            _actionId.value = id
            _actionError.value = null
            service?.deleteDatabaseServer(id)
                ?.onSuccess { _servers.value = _servers.value.filter { it.id != id } }
                ?.onFailure { _actionError.value = "删除失败: ${it.message}" }
            _actionId.value = null
        }
    }

    fun syncServer(id: Long) {
        viewModelScope.launch {
            _actionId.value = id
            _actionError.value = null
            service?.syncDatabaseServer(id)
                ?.onSuccess { _actionError.value = null }
                ?.onFailure { _actionError.value = "同步失败: ${it.message}" }
            _actionId.value = null
        }
    }

    override fun onCleared() {
        service?.close()
        super.onCleared()
    }
}
