package com.acepanel.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.acepanel.app.data.DatabaseApiItem
import com.acepanel.app.data.DatabaseServerApiItem
import com.acepanel.app.data.PanelRepository
import com.acepanel.app.network.PanelApiService

class DatabaseDetailViewModel : ViewModel() {

    private val _database = MutableStateFlow<DatabaseApiItem?>(null)
    val database: StateFlow<DatabaseApiItem?> = _database

    private val _server = MutableStateFlow<DatabaseServerApiItem?>(null)
    val server: StateFlow<DatabaseServerApiItem?> = _server

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private var service: PanelApiService? = null
    private var serverId: Long = 0
    private var databaseName: String = ""

    fun init(panelId: String, serverId: Long, databaseName: String) {
        this.serverId = serverId
        this.databaseName = databaseName
        val config = PanelRepository.getPanel(panelId) ?: return
        service?.close()
        service = PanelApiService(config)
        load()
    }

    private fun load() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            service?.getDatabases(page = 1, limit = 200)?.onSuccess { resp ->
                val db = resp.items.find { it.server_id == serverId && it.name == databaseName }
                _database.value = db
                if (db != null && db.server_id > 0) {
                    loadServer(db.server_id)
                } else {
                    _server.value = null
                    _error.value = "未找到数据库: $databaseName"
                }
            }?.onFailure { _error.value = it.message }
            _isLoading.value = false
        }
    }

    private suspend fun loadServer(serverId: Long) {
        service?.getDatabaseServers(page = 1, limit = 50)?.onSuccess { resp ->
            _server.value = resp.items.find { it.id == serverId }
        }
    }

    override fun onCleared() {
        service?.close()
        super.onCleared()
    }
}
