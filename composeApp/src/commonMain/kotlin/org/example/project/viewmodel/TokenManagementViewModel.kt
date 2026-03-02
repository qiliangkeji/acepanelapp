package org.example.project.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.example.project.data.CreateTokenRequest
import org.example.project.data.PanelRepository
import org.example.project.data.UserTokenItem
import org.example.project.network.PanelApiService

class TokenManagementViewModel : ViewModel() {

    private val _tokens = MutableStateFlow<List<UserTokenItem>>(emptyList())
    val tokens: StateFlow<List<UserTokenItem>> = _tokens

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _actionId = MutableStateFlow<Long?>(null)
    val actionId: StateFlow<Long?> = _actionId

    private val _actionError = MutableStateFlow<String?>(null)
    val actionError: StateFlow<String?> = _actionError

    private val _createResult = MutableStateFlow<String?>(null)
    val createResult: StateFlow<String?> = _createResult

    private val _showEditDialog = MutableStateFlow<UserTokenItem?>(null)
    val showEditDialog: StateFlow<UserTokenItem?> = _showEditDialog

    private var service: PanelApiService? = null
    private var currentUserId: Long = 0L

    fun init(panelId: String, userId: Long = 0L) {
        val cfg = PanelRepository.getPanel(panelId) ?: return
        service?.close()
        service = PanelApiService(cfg)
        currentUserId = userId
        viewModelScope.launch {
            // 若未传入 userId，先从 /api/user/info 获取当前用户 ID
            if (currentUserId <= 0L) {
                service?.getUserInfo()
                    ?.onSuccess { currentUserId = it.id }
            }
            load()
        }
    }

    fun load() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            service?.getTokens(currentUserId, limit = 50)
                ?.onSuccess { _tokens.value = it.items }
                ?.onFailure { _error.value = it.message }
            _isLoading.value = false
        }
    }

    fun createToken(expiredAtMs: Long, ips: List<String> = emptyList()) {
        viewModelScope.launch {
            _actionError.value = null
            _createResult.value = null
            service?.createToken(currentUserId, ips, expiredAtMs)
                ?.onSuccess { item ->
                    _tokens.value = _tokens.value + item
                    _createResult.value = item.token
                }
                ?.onFailure { _actionError.value = "创建失败: ${it.message}" }
        }
    }

    fun clearCreateResult() {
        _createResult.value = null
    }

    fun showEdit(token: UserTokenItem) {
        _actionError.value = null
        _showEditDialog.value = token
    }

    fun hideEdit() {
        _showEditDialog.value = null
    }

    fun updateToken(id: Long, ips: List<String>, expiredAtMs: Long) {
        viewModelScope.launch {
            _actionId.value = id
            _actionError.value = null
            service?.updateToken(id, ips, expiredAtMs)
                ?.onSuccess {
                    _tokens.value = _tokens.value.map {
                        if (it.id == id) it.copy(ips = ips, expired_at = expiredAtMs) else it
                    }
                    _showEditDialog.value = null
                }
                ?.onFailure { _actionError.value = "更新失败: ${it.message}" }
            _actionId.value = null
        }
    }

    fun deleteToken(id: Long) {
        viewModelScope.launch {
            _actionId.value = id
            _actionError.value = null
            service?.deleteToken(id)
                ?.onSuccess { _tokens.value = _tokens.value.filter { it.id != id } }
                ?.onFailure { _actionError.value = "删除失败: ${it.message}" }
            _actionId.value = null
        }
    }

    override fun onCleared() {
        service?.close()
        super.onCleared()
    }
}
