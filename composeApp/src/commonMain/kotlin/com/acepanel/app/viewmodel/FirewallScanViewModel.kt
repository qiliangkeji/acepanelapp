package com.acepanel.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.acepanel.app.data.FirewallScanSetting
import com.acepanel.app.data.FirewallScanSummary
import com.acepanel.app.data.PanelRepository
import com.acepanel.app.data.ScanEvent
import com.acepanel.app.data.ScanTopItem
import com.acepanel.app.network.PanelApiService
import com.acepanel.app.network.currentTimeSeconds

class FirewallScanViewModel : ViewModel() {

    private val _setting = MutableStateFlow<FirewallScanSetting?>(null)
    val setting: StateFlow<FirewallScanSetting?> = _setting

    private val _summary = MutableStateFlow<FirewallScanSummary?>(null)
    val summary: StateFlow<FirewallScanSummary?> = _summary

    private val _topIps = MutableStateFlow<List<ScanTopItem>>(emptyList())
    val topIps: StateFlow<List<ScanTopItem>> = _topIps

    private val _topPorts = MutableStateFlow<List<ScanTopItem>>(emptyList())
    val topPorts: StateFlow<List<ScanTopItem>> = _topPorts

    private val _events = MutableStateFlow<List<ScanEvent>>(emptyList())
    val events: StateFlow<List<ScanEvent>> = _events

    private val _eventTotal = MutableStateFlow(0L)
    val eventTotal: StateFlow<Long> = _eventTotal

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _actionError = MutableStateFlow<String?>(null)
    val actionError: StateFlow<String?> = _actionError

    val startDate = MutableStateFlow(daysAgoDate(30))
    val endDate = MutableStateFlow(daysAgoDate(0))

    private var eventPage = 1
    private val eventLimit = 20

    private var service: PanelApiService? = null

    fun init(panelId: String) {
        val cfg = PanelRepository.getPanel(panelId) ?: return
        service?.close()
        service = PanelApiService(cfg)
        viewModelScope.launch { loadSetting() }
        load()
    }

    private suspend fun loadSetting() {
        service?.getFirewallScanSetting()
            ?.onSuccess { _setting.value = it }
            ?.onFailure { /* 忽略 */ }
    }

    fun load() {
        val start = startDate.value
        val end = endDate.value
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            eventPage = 1
            launch {
                service?.getFirewallScanSummary(start, end)
                    ?.onSuccess { _summary.value = it }
                    ?.onFailure { _error.value = it.message }
            }
            launch {
                service?.getFirewallScanTopIps(start, end)
                    ?.onSuccess { _topIps.value = it }
                    ?.onFailure { }
            }
            launch {
                service?.getFirewallScanTopPorts(start, end)
                    ?.onSuccess { _topPorts.value = it }
                    ?.onFailure { }
            }
            launch {
                service?.getFirewallScanEvents(start, end, page = 1, limit = eventLimit)
                    ?.onSuccess { resp ->
                        _events.value = resp.items
                        _eventTotal.value = resp.total
                    }
                    ?.onFailure { }
            }
            _isLoading.value = false
        }
    }

    fun loadMoreEvents() {
        val start = startDate.value
        val end = endDate.value
        val nextPage = eventPage + 1
        viewModelScope.launch {
            service?.getFirewallScanEvents(start, end, page = nextPage, limit = eventLimit)
                ?.onSuccess { resp ->
                    _events.value = _events.value + resp.items
                    eventPage = nextPage
                }
                ?.onFailure { _actionError.value = it.message }
        }
    }

    fun toggleEnabled() {
        val current = _setting.value ?: return
        val updated = current.copy(enabled = !current.enabled)
        viewModelScope.launch {
            _actionError.value = null
            service?.setFirewallScanSetting(updated)
                ?.onSuccess { _setting.value = updated }
                ?.onFailure { _actionError.value = it.message }
        }
    }

    fun clearData() {
        viewModelScope.launch {
            _actionError.value = null
            service?.clearFirewallScanData()
                ?.onSuccess {
                    _summary.value = null
                    _topIps.value = emptyList()
                    _topPorts.value = emptyList()
                    _events.value = emptyList()
                    _eventTotal.value = 0L
                }
                ?.onFailure { _actionError.value = "清空失败: ${it.message}" }
        }
    }

    override fun onCleared() {
        service?.close()
        super.onCleared()
    }

    private fun daysAgoDate(days: Int): String {
        val epochDay = currentTimeSeconds() / 86_400L - days
        return civilFromDays(epochDay)
    }

    private fun civilFromDays(epochDay: Long): String {
        var z = epochDay + 719468L
        val era = if (z >= 0) z / 146097L else (z - 146096L) / 146097L
        val doe = z - era * 146097L
        val yoe = (doe - doe / 1460L + doe / 36524L - doe / 146096L) / 365L
        var y = yoe + era * 400L
        val doy = doe - (365L * yoe + yoe / 4L - yoe / 100L)
        val mp = (5L * doy + 2L) / 153L
        val d = doy - (153L * mp + 2L) / 5L + 1L
        val m = mp + if (mp < 10L) 3L else -9L
        y += if (m <= 2L) 1L else 0L
        return "${y.toString().padStart(4, '0')}-${m.toString().padStart(2, '0')}-${d.toString().padStart(2, '0')}"
    }
}
