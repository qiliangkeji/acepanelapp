package org.example.project.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.example.project.data.DatabaseApiItem
import org.example.project.data.DatabaseServerApiItem
import org.example.project.data.PanelRepository
import org.example.project.network.PanelApiService

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
    private var databaseId: Long = 0

    fun init(panelId: String, id: Long) {
        databaseId = id
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
                val db = resp.items.find { it.id == databaseId }
                _database.value = db
                if (db != null && db.server_id > 0) {
                    loadServer(db.server_id)
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
