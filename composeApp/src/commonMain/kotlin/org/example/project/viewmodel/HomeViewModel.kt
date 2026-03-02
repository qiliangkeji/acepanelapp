package org.example.project.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.example.project.data.PanelConfig
import org.example.project.data.PanelRepository
import org.example.project.data.PanelRuntimeStatus
import org.example.project.network.PanelApiService

class HomeViewModel : ViewModel() {

    private val _panels = MutableStateFlow<List<PanelConfig>>(emptyList())
    val panels: StateFlow<List<PanelConfig>> = _panels

    private val _statuses = MutableStateFlow<Map<String, PanelRuntimeStatus>>(emptyMap())
    val statuses: StateFlow<Map<String, PanelRuntimeStatus>> = _statuses

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    private var refreshJob: Job? = null
    private val statusMutex = Mutex()

    fun loadPanels() {
        _panels.value = PanelRepository.getPanels()
        refreshStatuses()
    }

    /** 并发刷新所有面板的运行状态 */
    fun refreshStatuses() {
        val currentPanels = _panels.value
        if (currentPanels.isEmpty()) {
            _statuses.value = emptyMap()
            _isRefreshing.value = false
            return
        }
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            _isRefreshing.value = true
            try {
                val panelIds = currentPanels.map { it.id }.toSet()
                val resultMap = _statuses.value
                    .filterKeys { it in panelIds }
                    .toMutableMap()
                _statuses.value = resultMap.toMap()

                coroutineScope {
                    currentPanels.forEach { panel ->
                        launch {
                            val status = fetchPanelStatus(panel)
                            statusMutex.withLock {
                                resultMap[panel.id] = status
                                _statuses.value = resultMap.toMap()
                            }
                        }
                    }
                }
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    private suspend fun fetchPanelStatus(config: PanelConfig): PanelRuntimeStatus {
        return try {
            val service = PanelApiService(config)
            val usageResult = service.getCurrentUsage()
            service.close()

            val usage = usageResult.getOrNull()
            val host = usage?.host

            if (usage != null) {
                val osName = listOf(host?.platform.orEmpty(), host?.platformVersion.orEmpty())
                    .filter { it.isNotBlank() }
                    .joinToString(" ")
                PanelRuntimeStatus(
                    isOnline = true,
                    cpuPercent = usage.percent,
                    memPercent = usage.mem.usedPercent,
                    diskPercent = usage.disk_usage.maxOfOrNull { it.usedPercent } ?: 0.0,
                    uptime = host?.uptime ?: 0,
                    hostname = host?.hostname.orEmpty(),
                    osName = osName
                )
            } else {
                PanelRuntimeStatus(isOnline = false)
            }
        } catch (e: Exception) {
            PanelRuntimeStatus(isOnline = false)
        }
    }

    fun removePanel(id: String) {
        PanelRepository.removePanel(id)
        loadPanels()
    }

    override fun onCleared() {
        refreshJob?.cancel()
        super.onCleared()
    }
}
