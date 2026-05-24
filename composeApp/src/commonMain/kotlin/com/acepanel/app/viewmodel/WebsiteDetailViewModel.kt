package com.acepanel.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.acepanel.app.data.PanelRepository
import com.acepanel.app.data.WebsiteDetailData
import com.acepanel.app.network.PanelApiService

class WebsiteDetailViewModel : ViewModel() {

    private val _website = MutableStateFlow<WebsiteDetailData?>(null)
    val website: StateFlow<WebsiteDetailData?> = _website

    private val _webserver = MutableStateFlow("")
    val webserver: StateFlow<String> = _webserver

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _actionError = MutableStateFlow<String?>(null)
    val actionError: StateFlow<String?> = _actionError

    private val _isActing = MutableStateFlow(false)
    val isActing: StateFlow<Boolean> = _isActing

    private var service: PanelApiService? = null
    private var websiteId: Long = 0

    fun init(panelId: String, id: Long) {
        websiteId = id
        val config = PanelRepository.getPanel(panelId) ?: return
        service?.close()
        service = PanelApiService(config)
        load()
        loadWebserver()
    }

    fun load() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            service?.getWebsiteDetail(websiteId)
                ?.onSuccess { _website.value = it }
                ?.onFailure { _error.value = it.message }
            _isLoading.value = false
        }
    }

    fun setStatus(status: Boolean) {
        viewModelScope.launch {
            _isActing.value = true
            _actionError.value = null
            service?.setWebsiteStatus(websiteId, status)
                ?.onSuccess { load() }
                ?.onFailure { _actionError.value = it.message }
            _isActing.value = false
        }
    }

    private fun loadWebserver() {
        viewModelScope.launch {
            service?.getInstalledEnvironment()
                ?.onSuccess { _webserver.value = it.webserver }
        }
    }

    override fun onCleared() {
        service?.close()
        super.onCleared()
    }
}
