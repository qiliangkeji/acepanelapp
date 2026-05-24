package com.acepanel.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.acepanel.app.data.CreateCronRequest
import com.acepanel.app.data.CronTaskApiItem
import com.acepanel.app.data.UpdateCronRequest
import com.acepanel.app.data.PanelRepository
import com.acepanel.app.network.PanelApiService
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

class CronTaskViewModel : ViewModel() {

    private val _tasks = MutableStateFlow<List<CronTaskApiItem>>(emptyList())
    val tasks: StateFlow<List<CronTaskApiItem>> = _tasks

    private val _total = MutableStateFlow(0)
    val total: StateFlow<Int> = _total

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _actionError = MutableStateFlow<String?>(null)
    val actionError: StateFlow<String?> = _actionError

    private val _showCreateDialog = MutableStateFlow(false)
    val showCreateDialog: StateFlow<Boolean> = _showCreateDialog

    private var service: PanelApiService? = null

    fun init(panelId: String) {
        val config = PanelRepository.getPanel(panelId) ?: return
        service?.close()
        service = PanelApiService(config)
        load()
    }

    fun load() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            service?.getCronTasks(limit = 50)?.onSuccess {
                _tasks.value = it.items
                _total.value = it.total
            }?.onFailure {
                _error.value = it.message
            }
            _isLoading.value = false
        }
    }

    fun toggleStatus(task: CronTaskApiItem) {
        viewModelScope.launch {
            val newStatus = !task.status
            service?.setCronTaskStatus(task.id, newStatus)?.onSuccess {
                _tasks.value = _tasks.value.map {
                    if (it.id == task.id) it.copy(status = newStatus) else it
                }
            }?.onFailure {
                _error.value = it.message
            }
        }
    }

    fun deleteTask(id: Long) {
        viewModelScope.launch {
            service?.deleteCronTask(id)?.onSuccess {
                _tasks.value = _tasks.value.filter { it.id != id }
                _total.value = _total.value - 1
            }?.onFailure {
                _error.value = it.message
            }
        }
    }

    private val _editingTask = MutableStateFlow<CronTaskApiItem?>(null)
    val editingTask: StateFlow<CronTaskApiItem?> = _editingTask

    fun showCreate() { _showCreateDialog.value = true; _actionError.value = null }
    fun hideCreate() { _showCreateDialog.value = false; _actionError.value = null }
    @OptIn(ExperimentalEncodingApi::class)
    fun showEdit(task: CronTaskApiItem) {
        _actionError.value = null
        viewModelScope.launch {
            service?.getCronTask(task.id)
                ?.onSuccess { detail ->
                    if (detail.type == "shell" && detail.shell.isNotBlank()) {
                        service?.getFileContent(detail.shell)
                            ?.onSuccess { file ->
                                val script = runCatching { Base64.decode(file.content).decodeToString() }
                                    .getOrElse { file.content }
                                _editingTask.value = detail.copy(log = script)
                            }
                            ?.onFailure {
                                _editingTask.value = detail
                                _actionError.value = "读取脚本失败: ${it.message}"
                            }
                    } else {
                        _editingTask.value = detail
                    }
                }
                ?.onFailure { _actionError.value = it.message }
        }
    }
    fun hideEdit() { _editingTask.value = null; _actionError.value = null }

    fun updateTask(id: Long, name: String, cronTime: String, keep: Int,
                   script: String, url: String, subType: String, targets: List<String>) {
        viewModelScope.launch {
            _actionError.value = null
            val taskType = _tasks.value.firstOrNull { it.id == id }?.type ?: "shell"
            val req = UpdateCronRequest(
                name = name,
                type = taskType,
                time = cronTime,
                keep = keep,
                script = script,
                url = url,
                method = if (taskType == "url") "GET" else "",
                sub_type = subType,
                targets = targets
            )
            service?.updateCronTask(id, req)?.onSuccess {
                _editingTask.value = null
                load()
            }?.onFailure { _actionError.value = it.message }
        }
    }

    fun createTask(name: String, type: String, cronTime: String, keep: Int,
                   script: String, url: String, subType: String, targets: List<String>) {
        viewModelScope.launch {
            val req = CreateCronRequest(
                name = name, type = type, time = cronTime, keep = keep,
                script = script, url = url, sub_type = subType, targets = targets
            )
            service?.createCronTask(req)?.onSuccess {
                _showCreateDialog.value = false
                load()
            }?.onFailure {
                _actionError.value = it.message
            }
        }
    }

    override fun onCleared() {
        service?.close()
        super.onCleared()
    }
}
