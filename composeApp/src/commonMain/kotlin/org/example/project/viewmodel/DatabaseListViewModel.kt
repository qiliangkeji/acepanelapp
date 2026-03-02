package org.example.project.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.example.project.data.DatabaseApiItem
import org.example.project.data.DatabaseServerApiItem
import org.example.project.data.PanelRepository
import org.example.project.network.PanelApiService

class DatabaseListViewModel : ViewModel() {

    private val _databases = MutableStateFlow<List<DatabaseApiItem>>(emptyList())
    val databases: StateFlow<List<DatabaseApiItem>> = _databases

    private val _servers = MutableStateFlow<List<DatabaseServerApiItem>>(emptyList())
    val servers: StateFlow<List<DatabaseServerApiItem>> = _servers

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private var service: PanelApiService? = null
    private var loadJob: Job? = null

    fun init(panelId: String) {
        val cfg = PanelRepository.getPanel(panelId) ?: return
        service?.close()
        service = PanelApiService(cfg)
        loadAll()
    }

    fun loadAll() {
        val svc = service ?: return
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                coroutineScope {
                    val serversDeferred = async { svc.getDatabaseServers() }
                    val databasesDeferred = async { svc.getDatabases() }

                    serversDeferred.await()
                        .onSuccess { _servers.value = it.items }
                        .onFailure { _error.value = "服务器列表加载失败: ${it.message}" }

                    databasesDeferred.await()
                        .onSuccess { _databases.value = it.items }
                        .onFailure {
                            if (_error.value == null) _error.value = "数据库列表加载失败: ${it.message}"
                        }
                }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteDatabase(serverId: Long, name: String) {
        val svc = service ?: return
        viewModelScope.launch {
            svc.deleteDatabase(serverId, name).onSuccess {
                _databases.value = _databases.value.filter { it.name != name || it.server_id != serverId }
            }.onFailure { _error.value = "删除失败: ${it.message}" }
        }
    }

    override fun onCleared() {
        loadJob?.cancel()
        service?.close()
        service = null
        super.onCleared()
    }
}
