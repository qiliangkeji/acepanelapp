package com.acepanel.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.acepanel.app.data.PanelConfig
import com.acepanel.app.data.PanelRepository
import com.acepanel.app.network.PanelApiService

class StatsViewModel : ViewModel() {

    data class PanelStat(
        val config: PanelConfig,
        val isOnline: Boolean,
        val cpuPercent: Double,
        val memPercent: Double,
        val diskPercent: Double,
        val websiteCount: Int,
        val dbCount: Int,
        val cronCount: Int
    )

    private val _panelStats = MutableStateFlow<List<PanelStat>>(emptyList())
    val panelStats: StateFlow<List<PanelStat>> = _panelStats

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun load() {
        val panels = PanelRepository.getPanels()
        if (panels.isEmpty()) {
            _panelStats.value = emptyList()
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            val stats = coroutineScope {
                panels.map { cfg ->
                    async {
                        val service = PanelApiService(cfg)
                        try {
                            val usageDeferred = async { service.getCurrentUsage().getOrNull() }
                            val countDeferred = async { service.getCountInfo().getOrNull() }
                            val usage = usageDeferred.await()
                            val countInfo = countDeferred.await()
                            PanelStat(
                                config = cfg,
                                isOnline = usage != null,
                                cpuPercent = usage?.percent ?: 0.0,
                                memPercent = usage?.mem?.usedPercent ?: 0.0,
                                diskPercent = usage?.disk_usage?.maxOfOrNull { it.usedPercent } ?: 0.0,
                                websiteCount = countInfo?.website ?: 0,
                                dbCount = countInfo?.database ?: 0,
                                cronCount = countInfo?.cron ?: 0
                            )
                        } catch (_: Exception) {
                            PanelStat(config = cfg, isOnline = false, cpuPercent = 0.0,
                                memPercent = 0.0, diskPercent = 0.0, websiteCount = 0,
                                dbCount = 0, cronCount = 0)
                        } finally {
                            service.close()
                        }
                    }
                }.awaitAll()
            }
            _panelStats.value = stats
            _isLoading.value = false
        }
    }
}
