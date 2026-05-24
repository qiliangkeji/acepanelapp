package com.acepanel.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.acepanel.app.data.CreateUserRequest
import com.acepanel.app.data.PanelRepository
import com.acepanel.app.data.TwoFaInfo
import com.acepanel.app.data.UserItem
import com.acepanel.app.network.PanelApiService

class UserManagementViewModel : ViewModel() {

    private val _users = MutableStateFlow<List<UserItem>>(emptyList())
    val users: StateFlow<List<UserItem>> = _users

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

    // 编辑用户
    private val _editUser = MutableStateFlow<UserItem?>(null)
    val editUser: StateFlow<UserItem?> = _editUser

    // 2FA 信息
    private val _twoFaInfo = MutableStateFlow<TwoFaInfo?>(null)
    val twoFaInfo: StateFlow<TwoFaInfo?> = _twoFaInfo

    private val _isTwoFaLoading = MutableStateFlow(false)
    val isTwoFaLoading: StateFlow<Boolean> = _isTwoFaLoading

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
            service?.getUsers(limit = 50)
                ?.onSuccess { _users.value = it.items }
                ?.onFailure { _error.value = it.message }
            _isLoading.value = false
        }
    }

    fun showCreate() { _showCreateDialog.value = true }
    fun hideCreate() { _showCreateDialog.value = false }

    fun createUser(username: String, password: String, email: String) {
        viewModelScope.launch {
            _actionError.value = null
            service?.createUser(CreateUserRequest(username, password, email))
                ?.onSuccess {
                    hideCreate()
                    load()
                }
                ?.onFailure { _actionError.value = it.message }
        }
    }

    fun deleteUser(id: Long) {
        viewModelScope.launch {
            _actionId.value = id
            _actionError.value = null
            service?.deleteUser(id)
                ?.onSuccess { _users.value = _users.value.filter { it.id != id } }
                ?.onFailure { _actionError.value = "删除失败: ${it.message}" }
            _actionId.value = null
        }
    }

    fun showEdit(user: UserItem) { _editUser.value = user }
    fun hideEdit() { _editUser.value = null; _actionError.value = null }

    fun updateUsername(id: Long, username: String) {
        viewModelScope.launch {
            _actionError.value = null
            service?.updateUsername(id, username)
                ?.onSuccess { load(); hideEdit() }
                ?.onFailure { _actionError.value = it.message }
        }
    }

    fun updatePassword(id: Long, password: String) {
        viewModelScope.launch {
            _actionError.value = null
            service?.updatePassword(id, password)
                ?.onSuccess { hideEdit() }
                ?.onFailure { _actionError.value = it.message }
        }
    }

    fun updateEmail(id: Long, email: String) {
        viewModelScope.launch {
            _actionError.value = null
            service?.updateEmail(id, email)
                ?.onSuccess { load(); hideEdit() }
                ?.onFailure { _actionError.value = it.message }
        }
    }

    fun loadTwoFa(id: Long) {
        viewModelScope.launch {
            _isTwoFaLoading.value = true
            _actionError.value = null
            service?.getUserTwoFa(id)
                ?.onSuccess { _twoFaInfo.value = it }
                ?.onFailure { _actionError.value = it.message }
            _isTwoFaLoading.value = false
        }
    }

    fun enableTwoFa(id: Long, secret: String, code: String) {
        viewModelScope.launch {
            _actionError.value = null
            service?.setUserTwoFa(id, secret, code)
                ?.onSuccess { _twoFaInfo.value = null }
                ?.onFailure { _actionError.value = it.message }
        }
    }

    fun disableTwoFa(id: Long) {
        viewModelScope.launch {
            _actionError.value = null
            service?.setUserTwoFa(id, "", "")
                ?.onSuccess { _twoFaInfo.value = null }
                ?.onFailure { _actionError.value = it.message }
        }
    }

    fun hideTwoFa() { _twoFaInfo.value = null; _actionError.value = null }

    override fun onCleared() {
        service?.close()
        super.onCleared()
    }
}
