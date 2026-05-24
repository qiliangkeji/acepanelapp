package com.acepanel.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.acepanel.app.data.PanelRepository
import com.acepanel.app.data.PanelSettingData
import com.acepanel.app.network.PanelApiService

class PanelSettingsViewModel : ViewModel() {

    private val _setting = MutableStateFlow<PanelSettingData?>(null)
    val setting: StateFlow<PanelSettingData?> = _setting

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _saveError = MutableStateFlow<String?>(null)
    val saveError: StateFlow<String?> = _saveError

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving

    private val _restartRequired = MutableStateFlow(false)
    val restartRequired: StateFlow<Boolean> = _restartRequired

    // 便签
    private val _memo = MutableStateFlow("")
    val memo: StateFlow<String> = _memo

    private val _isMemoLoading = MutableStateFlow(false)
    val isMemoLoading: StateFlow<Boolean> = _isMemoLoading

    private var service: PanelApiService? = null

    fun init(panelId: String) {
        val config = PanelRepository.getPanel(panelId) ?: return
        service?.close()
        service = PanelApiService(config)
        load()
        loadMemo()
    }

    fun load() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            service?.getSettings()
                ?.onSuccess { _setting.value = it }
                ?.onFailure { _error.value = it.message }
            _isLoading.value = false
        }
    }

    fun loadMemo() {
        viewModelScope.launch {
            _isMemoLoading.value = true
            service?.getMemo()?.onSuccess { _memo.value = it }
            _isMemoLoading.value = false
        }
    }

    fun saveMemo(content: String) {
        viewModelScope.launch {
            service?.setMemo(content)?.onSuccess { _memo.value = content }
        }
    }

    /** 修改单个字段后全量保存 */
    private fun saveWith(block: PanelSettingData.() -> PanelSettingData) {
        val current = _setting.value ?: return
        val updated = current.block().normalizedForSave()
        viewModelScope.launch {
            _isSaving.value = true
            _saveError.value = null
            service?.updateSettings(updated)
                ?.onSuccess { resp ->
                    _setting.value = updated
                    _restartRequired.value = resp.restart
                }
                ?.onFailure { _saveError.value = it.message }
            _isSaving.value = false
        }
    }

    fun updatePort(port: Int) = saveWith { copy(port = port) }
    fun updateLifetime(minutes: Int) = saveWith { copy(lifetime = minutes) }
    fun updateEntrance(entrance: String) = saveWith { copy(entrance = entrance.ifBlank { "/" }) }
    fun updateEntranceError(mode: String) = saveWith { copy(entrance_error = mode) }
    fun updateAutoUpdate(enabled: Boolean) = saveWith { copy(auto_update = enabled) }
    fun updateLoginCaptcha(enabled: Boolean) = saveWith { copy(login_captcha = enabled) }
    fun updateChannel(channel: String) = saveWith { copy(channel = channel) }
    fun dismissRestart() { _restartRequired.value = false }

    private fun PanelSettingData.normalizedForSave(): PanelSettingData {
        return copy(
            entrance = entrance.ifBlank { "/" },
            backup_format = backup_format.ifBlank { "tar.xz" }
        )
    }

    override fun onCleared() {
        service?.close()
        super.onCleared()
    }
}
