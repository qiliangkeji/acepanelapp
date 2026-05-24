package com.acepanel.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.acepanel.app.data.CreateWebHookRequest
import com.acepanel.app.data.PanelRepository
import com.acepanel.app.data.UpdateWebHookRequest
import com.acepanel.app.data.WebHookItem
import com.acepanel.app.network.PanelApiService

class WebHookViewModel : ViewModel() {

    private val _hooks = MutableStateFlow<List<WebHookItem>>(emptyList())
    val hooks: StateFlow<List<WebHookItem>> = _hooks

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

    private val _showEditDialog = MutableStateFlow<WebHookItem?>(null)
    val showEditDialog: StateFlow<WebHookItem?> = _showEditDialog

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
            service?.getWebHooks(limit = 50)
                ?.onSuccess { _hooks.value = it.items }
                ?.onFailure { _error.value = it.message }
            _isLoading.value = false
        }
    }

    fun showCreate() { _showCreateDialog.value = true }
    fun hideCreate() { _showCreateDialog.value = false }
    fun showEdit(hook: WebHookItem) { _showEditDialog.value = hook; _actionError.value = null }
    fun hideEdit() { _showEditDialog.value = null }

    fun createHook(name: String, script: String, user: String, raw: Boolean) {
        viewModelScope.launch {
            _actionError.value = null
            service?.createWebHook(CreateWebHookRequest(name, script, raw, user))
                ?.onSuccess { item ->
                    _hooks.value = _hooks.value + item
                    hideCreate()
                }
                ?.onFailure { _actionError.value = it.message }
        }
    }

    fun toggleStatus(hook: WebHookItem) {
        viewModelScope.launch {
            _actionId.value = hook.id
            _actionError.value = null
            val req = UpdateWebHookRequest(hook.name, hook.script, hook.raw, hook.user.ifEmpty { "root" }, !hook.status)
            service?.updateWebHook(hook.id, req)
                ?.onSuccess {
                    _hooks.value = _hooks.value.map {
                        if (it.id == hook.id) it.copy(status = !hook.status) else it
                    }
                }
                ?.onFailure { _actionError.value = "操作失败: ${it.message}" }
            _actionId.value = null
        }
    }

    fun updateHook(id: Long, name: String, script: String, user: String, raw: Boolean, status: Boolean) {
        viewModelScope.launch {
            _actionId.value = id
            _actionError.value = null
            val req = UpdateWebHookRequest(name, script, raw, user.ifEmpty { "root" }, status)
            service?.updateWebHook(id, req)
                ?.onSuccess {
                    _hooks.value = _hooks.value.map {
                        if (it.id == id) it.copy(name = name, script = script, user = user.ifEmpty { "root" }, raw = raw, status = status) else it
                    }
                    _showEditDialog.value = null
                }
                ?.onFailure { _actionError.value = "更新失败: ${it.message}" }
            _actionId.value = null
        }
    }

    fun deleteHook(id: Long) {
        viewModelScope.launch {
            _actionId.value = id
            _actionError.value = null
            service?.deleteWebHook(id)
                ?.onSuccess { _hooks.value = _hooks.value.filter { it.id != id } }
                ?.onFailure { _actionError.value = "删除失败: ${it.message}" }
            _actionId.value = null
        }
    }

    override fun onCleared() {
        service?.close()
        super.onCleared()
    }
}
