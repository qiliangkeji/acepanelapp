package com.acepanel.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.acepanel.app.data.CreateSshRequest
import com.acepanel.app.data.PanelRepository
import com.acepanel.app.data.SshHostItem
import com.acepanel.app.network.PanelApiService

class SshHostViewModel : ViewModel() {

    private val _hosts = MutableStateFlow<List<SshHostItem>>(emptyList())
    val hosts: StateFlow<List<SshHostItem>> = _hosts

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

    private val _editHost = MutableStateFlow<SshHostItem?>(null)
    val editHost: StateFlow<SshHostItem?> = _editHost

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
            service?.getSshHosts(limit = 50)
                ?.onSuccess { _hosts.value = it.items }
                ?.onFailure { _error.value = it.message }
            _isLoading.value = false
        }
    }

    fun showCreate() { _showCreateDialog.value = true; _actionError.value = null }
    fun hideCreate() { _showCreateDialog.value = false }
    fun showEdit(host: SshHostItem) { _editHost.value = host; _actionError.value = null }
    fun hideEdit() { _editHost.value = null; _actionError.value = null }

    fun createHost(name: String, host: String, port: Int, authMethod: String, user: String, password: String, key: String) {
        viewModelScope.launch {
            _actionError.value = null
            service?.createSshHost(
                CreateSshRequest(name, host, port, authMethod, user, password, key)
            )?.onSuccess {
                hideCreate()
                load()
            }?.onFailure { _actionError.value = it.message }
        }
    }

    fun updateHost(id: Long, name: String, host: String, port: Int,
                   authMethod: String, user: String, password: String, key: String) {
        viewModelScope.launch {
            _actionError.value = null
            service?.updateSshHost(
                id, CreateSshRequest(name, host, port, authMethod, user, password, key)
            )?.onSuccess {
                hideEdit()
                load()
            }?.onFailure { _actionError.value = it.message }
        }
    }

    fun deleteHost(id: Long) {
        viewModelScope.launch {
            _actionId.value = id
            _actionError.value = null
            service?.deleteSshHost(id)
                ?.onSuccess { _hosts.value = _hosts.value.filter { it.id != id } }
                ?.onFailure { _actionError.value = "删除失败: ${it.message}" }
            _actionId.value = null
        }
    }

    override fun onCleared() {
        service?.close()
        super.onCleared()
    }
}
