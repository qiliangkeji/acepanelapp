package com.acepanel.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.acepanel.app.data.CreateDatabaseRequest
import com.acepanel.app.data.DatabaseServerApiItem
import com.acepanel.app.data.PanelRepository
import com.acepanel.app.network.PanelApiService

class AddDatabaseViewModel : ViewModel() {

    val servers = MutableStateFlow<List<DatabaseServerApiItem>>(emptyList())
    val selectedServerId = MutableStateFlow(0L)

    val dbName = MutableStateFlow("")
    val createUser = MutableStateFlow(false)
    val username = MutableStateFlow("")
    val password = MutableStateFlow("")
    val host = MutableStateFlow("localhost")
    val comment = MutableStateFlow("")

    val isLoadingServers = MutableStateFlow(false)

    private val _submitStatus = MutableStateFlow<SubmitStatus>(SubmitStatus.Idle)
    val submitStatus: StateFlow<SubmitStatus> = _submitStatus

    private var service: PanelApiService? = null

    fun init(panelId: String) {
        val config = PanelRepository.getPanel(panelId) ?: return
        service?.close()
        service = PanelApiService(config)
        loadServers()
    }

    private fun loadServers() {
        viewModelScope.launch {
            isLoadingServers.value = true
            service?.getDatabaseServers(limit = 50)?.onSuccess { resp ->
                servers.value = resp.items
                if (resp.items.isNotEmpty()) selectedServerId.value = resp.items.first().id
            }
            isLoadingServers.value = false
        }
    }

    fun submit(onSuccess: () -> Unit) {
        val n = dbName.value.trim()
        if (n.isEmpty()) {
            _submitStatus.value = SubmitStatus.Error("数据库名不能为空")
            return
        }
        if (!n.matches(Regex("^[a-zA-Z0-9_-]+$"))) {
            _submitStatus.value = SubmitStatus.Error("数据库名只能包含字母、数字、下划线和连字符")
            return
        }
        if (selectedServerId.value == 0L) {
            _submitStatus.value = SubmitStatus.Error("请选择数据库服务器")
            return
        }

        val req = CreateDatabaseRequest(
            server_id = selectedServerId.value,
            name = n,
            create_user = createUser.value,
            username = if (createUser.value) username.value.trim() else "",
            password = if (createUser.value) password.value else "",
            host = if (createUser.value) host.value.trim().ifEmpty { "localhost" } else "localhost",
            comment = comment.value.trim()
        )

        viewModelScope.launch {
            _submitStatus.value = SubmitStatus.Loading
            service?.createDatabaseEntry(req)
                ?.onSuccess {
                    _submitStatus.value = SubmitStatus.Success
                    onSuccess()
                }
                ?.onFailure { _submitStatus.value = SubmitStatus.Error(it.message ?: "未知错误") }
        }
    }

    sealed class SubmitStatus {
        object Idle : SubmitStatus()
        object Loading : SubmitStatus()
        object Success : SubmitStatus()
        data class Error(val message: String) : SubmitStatus()
    }

    override fun onCleared() {
        service?.close()
        super.onCleared()
    }
}
