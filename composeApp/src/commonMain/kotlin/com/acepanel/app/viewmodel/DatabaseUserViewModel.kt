package com.acepanel.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.acepanel.app.data.CreateDatabaseUserRequest
import com.acepanel.app.data.DatabaseServerApiItem
import com.acepanel.app.data.DatabaseUserApiItem
import com.acepanel.app.data.UpdateDatabaseUserRequest
import com.acepanel.app.data.PanelRepository
import com.acepanel.app.network.PanelApiService

class DatabaseUserViewModel : ViewModel() {

    private val _users = MutableStateFlow<List<DatabaseUserApiItem>>(emptyList())
    val users: StateFlow<List<DatabaseUserApiItem>> = _users

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

    private var service: PanelApiService? = null

    fun init(panelId: String) {
        val cfg = PanelRepository.getPanel(panelId) ?: return
        service?.close()
        service = PanelApiService(cfg)
        viewModelScope.launch {
            load()
            loadServers()
        }
    }

    fun load() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            service?.getDatabaseUsers(limit = 100)
                ?.onSuccess { _users.value = it.items }
                ?.onFailure { _error.value = it.message }
            _isLoading.value = false
        }
    }

    private suspend fun loadServers() {
        service?.getDatabaseServers(limit = 50)
            ?.onSuccess { _servers.value = it.items }
    }

    fun showCreate() { _showCreateDialog.value = true }
    fun hideCreate() { _showCreateDialog.value = false }

    fun createUser(serverId: Long, username: String, password: String, host: String, privileges: List<String>) {
        viewModelScope.launch {
            _actionError.value = null
            service?.createDatabaseUser(
                CreateDatabaseUserRequest(serverId, username, password, host, privileges)
            )?.onSuccess {
                hideCreate()
                load()
            }?.onFailure { _actionError.value = it.message }
        }
    }

    private val _editUser = MutableStateFlow<DatabaseUserApiItem?>(null)
    val editUser: StateFlow<DatabaseUserApiItem?> = _editUser

    fun showEdit(user: DatabaseUserApiItem) { _editUser.value = user; _actionError.value = null }
    fun hideEdit() { _editUser.value = null; _actionError.value = null }

    fun updateUser(id: Long, password: String, privileges: List<String>) {
        viewModelScope.launch {
            _actionId.value = id
            _actionError.value = null
            service?.updateDatabaseUser(id, UpdateDatabaseUserRequest(password, privileges))
                ?.onSuccess { _editUser.value = null; load() }
                ?.onFailure { _actionError.value = it.message }
            _actionId.value = null
        }
    }

    fun deleteUser(id: Long) {
        viewModelScope.launch {
            _actionId.value = id
            _actionError.value = null
            service?.deleteDatabaseUser(id)
                ?.onSuccess { _users.value = _users.value.filter { it.id != id } }
                ?.onFailure { _actionError.value = "删除失败: ${it.message}" }
            _actionId.value = null
        }
    }

    override fun onCleared() {
        service?.close()
        super.onCleared()
    }
}
