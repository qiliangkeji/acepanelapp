package org.example.project.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.example.project.data.PanelRepository
import org.example.project.data.WebsiteStatClientResponse
import org.example.project.data.WebsiteStatErrorItem
import org.example.project.data.WebsiteStatGeoItem
import org.example.project.data.WebsiteStatIpItem
import org.example.project.data.WebsiteStatOverview
import org.example.project.data.WebsiteStatRealtime
import org.example.project.data.WebsiteStatSiteItem
import org.example.project.data.WebsiteStatSpiderItem
import org.example.project.data.WebsiteStatSetting
import org.example.project.data.WebsiteStatUriItem
import org.example.project.network.PanelApiService

class WebsiteStatViewModel : ViewModel() {

    private val _overview = MutableStateFlow<WebsiteStatOverview?>(null)
    val overview: StateFlow<WebsiteStatOverview?> = _overview

    private val _setting = MutableStateFlow<WebsiteStatSetting?>(null)
    val setting: StateFlow<WebsiteStatSetting?> = _setting

    private val _realtime = MutableStateFlow<WebsiteStatRealtime?>(null)
    val realtime: StateFlow<WebsiteStatRealtime?> = _realtime

    private val _siteItems = MutableStateFlow<List<WebsiteStatSiteItem>>(emptyList())
    val siteItems: StateFlow<List<WebsiteStatSiteItem>> = _siteItems

    private val _spiderItems = MutableStateFlow<List<WebsiteStatSpiderItem>>(emptyList())
    val spiderItems: StateFlow<List<WebsiteStatSpiderItem>> = _spiderItems

    private val _clientStats = MutableStateFlow<WebsiteStatClientResponse?>(null)
    val clientStats: StateFlow<WebsiteStatClientResponse?> = _clientStats

    private val _ipItems = MutableStateFlow<List<WebsiteStatIpItem>>(emptyList())
    val ipItems: StateFlow<List<WebsiteStatIpItem>> = _ipItems

    private val _geoItems = MutableStateFlow<List<WebsiteStatGeoItem>>(emptyList())
    val geoItems: StateFlow<List<WebsiteStatGeoItem>> = _geoItems

    private val _uriItems = MutableStateFlow<List<WebsiteStatUriItem>>(emptyList())
    val uriItems: StateFlow<List<WebsiteStatUriItem>> = _uriItems

    private val _slowUriItems = MutableStateFlow<List<WebsiteStatUriItem>>(emptyList())
    val slowUriItems: StateFlow<List<WebsiteStatUriItem>> = _slowUriItems

    private val _errorItems = MutableStateFlow<List<WebsiteStatErrorItem>>(emptyList())
    val errorItems: StateFlow<List<WebsiteStatErrorItem>> = _errorItems

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _actionError = MutableStateFlow<String?>(null)
    val actionError: StateFlow<String?> = _actionError

    val startDate = MutableStateFlow("")
    val endDate = MutableStateFlow("")

    private var service: PanelApiService? = null
    private var loadJob: Job? = null

    fun init(panelId: String) {
        val cfg = PanelRepository.getPanel(panelId) ?: return
        loadJob?.cancel()
        service?.close()
        service = PanelApiService(cfg)
        viewModelScope.launch {
            loadOverview()
            loadSetting()
        }
    }

    fun loadOverview() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                service?.getWebsiteStatOverview(startDate.value, endDate.value)
                    ?.onSuccess { _overview.value = it }
                    ?.onFailure { _error.value = it.message }
                loadExtendedStats()
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun loadSetting() {
        service?.getWebsiteStatSetting()
            ?.onSuccess { _setting.value = it }
            ?.onFailure { /* 忽略设置加载失败 */ }
    }

    fun saveSetting(days: Int, bodyEnabled: Boolean) {
        val current = _setting.value ?: WebsiteStatSetting()
        viewModelScope.launch {
            _actionError.value = null
            service?.setWebsiteStatSetting(current.copy(days = days, body_enabled = bodyEnabled))
                ?.onSuccess { loadSetting() }
                ?.onFailure { _actionError.value = "保存失败: ${it.message}" }
        }
    }

    fun clearStats() {
        viewModelScope.launch {
            _actionError.value = null
            service?.clearWebsiteStats()
                ?.onSuccess {
                    _overview.value = null
                    _realtime.value = null
                    _siteItems.value = emptyList()
                    _spiderItems.value = emptyList()
                    _clientStats.value = null
                    _ipItems.value = emptyList()
                    _geoItems.value = emptyList()
                    _uriItems.value = emptyList()
                    _slowUriItems.value = emptyList()
                    _errorItems.value = emptyList()
                }
                ?.onFailure { _actionError.value = "清空失败: ${it.message}" }
        }
    }

    private suspend fun loadExtendedStats() {
        val start = startDate.value
        val end = endDate.value
        val svc = service ?: return
        coroutineScope {
            launch {
                svc.getWebsiteStatRealtime()
                    .onSuccess { _realtime.value = it }
            }
            launch {
                svc.getWebsiteStatSites(start, end)
                    .onSuccess { _siteItems.value = it.items }
            }
            launch {
                svc.getWebsiteStatSpiders(start, end)
                    .onSuccess { _spiderItems.value = it.items }
            }
            launch {
                svc.getWebsiteStatClients(start, end)
                    .onSuccess { _clientStats.value = it }
            }
            launch {
                svc.getWebsiteStatIps(start, end, page = 1, limit = 20)
                    .onSuccess { _ipItems.value = it.items }
            }
            launch {
                svc.getWebsiteStatGeos(start, end, limit = 20)
                    .onSuccess { _geoItems.value = it.items }
            }
            launch {
                svc.getWebsiteStatUris(start, end, page = 1, limit = 20)
                    .onSuccess { _uriItems.value = it.items }
            }
            launch {
                svc.getWebsiteStatSlowUris(start, end, threshold = 500, page = 1, limit = 20)
                    .onSuccess { _slowUriItems.value = it.items }
            }
            launch {
                svc.getWebsiteStatErrors(start, end, page = 1, limit = 20)
                    .onSuccess { _errorItems.value = it.items }
            }
        }
    }

    override fun onCleared() {
        loadJob?.cancel()
        service?.close()
        super.onCleared()
    }
}
