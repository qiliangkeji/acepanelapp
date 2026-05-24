package com.acepanel.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.acepanel.app.data.PanelRepository
import com.acepanel.app.data.TaskItem
import com.acepanel.app.network.PanelApiService

class TaskCenterViewModel : ViewModel() {

    private val _tasks = MutableStateFlow<List<TaskItem>>(emptyList())
    val tasks: StateFlow<List<TaskItem>> = _tasks

    private val _total = MutableStateFlow(0)
    val total: StateFlow<Int> = _total

    private val _hasRunning = MutableStateFlow(false)
    val hasRunning: StateFlow<Boolean> = _hasRunning

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private var service: PanelApiService? = null
    private var loadJob: Job? = null

    fun init(panelId: String) {
        val config = PanelRepository.getPanel(panelId) ?: return
        service?.close()
        service = PanelApiService(config)
        load()
    }

    fun load() {
        val svc = service ?: return
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                coroutineScope {
                    val statusDeferred = async { svc.getTaskStatus() }
                    val tasksDeferred = async { svc.getTasks(page = 1, limit = 50) }

                    statusDeferred.await().onSuccess { _hasRunning.value = it }
                    tasksDeferred.await().onSuccess { resp ->
                        _tasks.value = resp.items
                        _total.value = resp.total
                    }.onFailure { _error.value = it.message }
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteTask(id: Long) {
        viewModelScope.launch {
            service?.deleteTask(id)?.onSuccess {
                _tasks.value = _tasks.value.filter { it.id != id }
                _total.value = (_total.value - 1).coerceAtLeast(0)
            }
        }
    }

    override fun onCleared() {
        loadJob?.cancel()
        service?.close()
        super.onCleared()
    }
}
