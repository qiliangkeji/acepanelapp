package com.acepanel.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.acepanel.app.data.PanelConfig
import com.acepanel.app.data.PanelRepository
import com.acepanel.app.network.PanelApiService
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

class ConfigEditorViewModel : ViewModel() {

    val content = MutableStateFlow("")

    private val _config = MutableStateFlow<PanelConfig?>(null)
    val config: StateFlow<PanelConfig?> = _config

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _saveResult = MutableStateFlow<String?>(null)
    val saveResult: StateFlow<String?> = _saveResult

    private var service: PanelApiService? = null
    private var currentPath: String = ""
    private var currentService: String = ""

    fun init(panelId: String, filePath: String, serviceName: String = "") {
        val cfg = PanelRepository.getPanel(panelId) ?: return
        _config.value = cfg
        currentPath = filePath
        currentService = serviceName
        service?.close()
        service = PanelApiService(cfg)
        if (filePath.isNotEmpty() || serviceName.isNotEmpty()) {
            viewModelScope.launch { loadContent() }
        }
    }

    @OptIn(ExperimentalEncodingApi::class)
    private suspend fun loadContent() {
        _isLoading.value = true
        _error.value = null
        if (currentService.isNotBlank()) {
            service?.tailFile(service = currentService, limit = 800)
                ?.onSuccess { resp -> content.value = resp.lines.joinToString("\n") }
                ?.onFailure { _error.value = it.message }
        } else {
            service?.getFileContent(currentPath)
                ?.onSuccess { resp ->
                    content.value = try {
                        Base64.decode(resp.content).decodeToString()
                    } catch (_: Exception) {
                        resp.content
                    }
                }
                ?.onFailure { _error.value = it.message }
        }
        _isLoading.value = false
    }

    fun save(onSuccess: () -> Unit) {
        if (currentService.isNotBlank()) return
        viewModelScope.launch {
            _isSaving.value = true
            _saveResult.value = null
            service?.saveFile(currentPath, content.value)
                ?.onSuccess {
                    _saveResult.value = "保存成功"
                    onSuccess()
                }
                ?.onFailure { _saveResult.value = "保存失败: ${it.message}" }
            _isSaving.value = false
        }
    }

    override fun onCleared() {
        service?.close()
        super.onCleared()
    }
}
