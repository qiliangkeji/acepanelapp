package com.acepanel.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.acepanel.app.data.PanelConfig
import com.acepanel.app.data.PanelRepository
import com.acepanel.app.data.SystemInfo
import com.acepanel.app.data.CurrentUsage
import com.acepanel.app.network.PanelApiService

class PanelDetailViewModel : ViewModel() {

    private val _config = MutableStateFlow<PanelConfig?>(null)
    val config: StateFlow<PanelConfig?> = _config

    private val _systemInfo = MutableStateFlow<SystemInfo?>(null)
    val systemInfo: StateFlow<SystemInfo?> = _systemInfo

    private val _currentUsage = MutableStateFlow<CurrentUsage?>(null)
    val currentUsage: StateFlow<CurrentUsage?> = _currentUsage

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private var service: PanelApiService? = null
    private var pollingJob: Job? = null

    fun init(panelId: String) {
        val cfg = PanelRepository.getPanel(panelId) ?: return
        _config.value = cfg
        pollingJob?.cancel()
        service?.close()
        service = PanelApiService(cfg)
        loadData()
        startPolling()
    }

    private fun loadData() {
        val svc = service ?: return
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val usageResult = svc.getCurrentUsage()
                usageResult
                    .onSuccess { usage ->
                        _currentUsage.value = usage
                        val host = usage.host
                        val osName = listOf(host.platform, host.platformVersion)
                            .filter { it.isNotBlank() }
                            .joinToString(" ")
                        _systemInfo.value = SystemInfo(
                            hostname = host.hostname,
                            os_name = osName,
                            uptime = host.uptime
                        )
                    }
                    .onFailure { _error.value = friendlyError(it) }
            } catch (e: Exception) {
                _error.value = friendlyError(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /** 每 5 秒自动刷新资源使用率 */
    private fun startPolling() {
        val svc = service ?: return
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (isActive) {
                delay(5000)
                try {
                    svc.getCurrentUsage().onSuccess { _currentUsage.value = it }
                } catch (_: Exception) {}
            }
        }
    }

    fun refresh() = loadData()

    private fun friendlyError(e: Throwable): String {
        val msg = e.message ?: return "未知错误"
        return when {
            msg.contains("timeout", ignoreCase = true) ||
            msg.contains("timed out", ignoreCase = true) ->
                "连接超时（8s），请检查网络或服务器是否可达"
            msg.contains("Unable to resolve host") ||
            msg.contains("No address associated") ->
                "无法解析主机，请检查域名或 DNS 设置"
            msg.contains("Connection refused") ->
                "连接被拒绝，请确认服务器端口是否开放"
            msg.contains("Network is unreachable") ||
            msg.contains("ENETUNREACH") ->
                "网络不可达，请检查设备网络连接"
            else -> msg
        }
    }

    override fun onCleared() {
        pollingJob?.cancel()
        service?.close()
        service = null
        super.onCleared()
    }
}
