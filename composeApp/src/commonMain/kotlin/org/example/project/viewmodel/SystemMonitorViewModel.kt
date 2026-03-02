package org.example.project.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.example.project.data.CurrentUsage
import org.example.project.data.MonitorHistoryResponse
import org.example.project.data.MonitorSetting
import org.example.project.data.PanelRepository
import org.example.project.data.ProcessApiItem
import org.example.project.data.ProcessStatItem
import org.example.project.network.PanelApiService

class SystemMonitorViewModel : ViewModel() {

    val currentUsage = MutableStateFlow<CurrentUsage?>(null)
    val topProcesses = MutableStateFlow<List<ProcessStatItem>>(emptyList())
    val processType = MutableStateFlow("cpu") // cpu / memory / disk_io
    val isLoading = MutableStateFlow(false)
    val error = MutableStateFlow<String?>(null)

    // 网络速率（byte/s），基于相邻两次采集差值
    val netSentSpeed = MutableStateFlow(0L)
    val netRecvSpeed = MutableStateFlow(0L)

    // 进程列表
    val processList = MutableStateFlow<List<ProcessApiItem>>(emptyList())
    val processTotal = MutableStateFlow(0L)
    val isProcessLoading = MutableStateFlow(false)
    val processSort = MutableStateFlow("cpu") // cpu/rss/pid/name

    // 监控历史
    val monitorHistory = MutableStateFlow<MonitorHistoryResponse?>(null)
    val isHistoryLoading = MutableStateFlow(false)

    // 监控设置
    val monitorSetting = MutableStateFlow<MonitorSetting?>(null)
    val showSettingDialog = MutableStateFlow(false)

    private var prevSentBytes = 0L
    private var prevRecvBytes = 0L

    private var service: PanelApiService? = null
    private var pollJob: Job? = null
    private val usageMutex = Mutex()

    fun init(panelId: String) {
        val config = PanelRepository.getPanel(panelId) ?: return
        service?.close()
        service = PanelApiService(config)
        startPolling()
        loadTopProcesses()
    }

    private fun startPolling() {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            while (isActive) {
                fetchCurrentUsage()
                delay(5000)
            }
        }
    }

    private suspend fun fetchCurrentUsage() {
        usageMutex.withLock {
            isLoading.value = currentUsage.value == null
            error.value = null
            service?.getCurrentUsage()?.onSuccess { usage ->
                currentUsage.value = usage
                val totalSent = usage.net.sumOf { it.bytesSent }
                val totalRecv = usage.net.sumOf { it.bytesRecv }
                if (prevSentBytes > 0) {
                    netSentSpeed.value = ((totalSent - prevSentBytes) / 5).coerceAtLeast(0)
                    netRecvSpeed.value = ((totalRecv - prevRecvBytes) / 5).coerceAtLeast(0)
                }
                prevSentBytes = totalSent
                prevRecvBytes = totalRecv
            }?.onFailure {
                if (currentUsage.value == null) error.value = it.message
            }
            isLoading.value = false
        }
    }

    fun loadTopProcesses(type: String = processType.value) {
        processType.value = type
        viewModelScope.launch {
            service?.getTopProcesses(type)
                ?.onSuccess { topProcesses.value = it }
        }
    }

    fun loadProcessList(sort: String = processSort.value) {
        processSort.value = sort
        viewModelScope.launch {
            isProcessLoading.value = true
            service?.getProcessList(page = 1, limit = 50, sort = sort, order = "desc")
                ?.onSuccess { resp ->
                    processList.value = resp.items
                    processTotal.value = resp.total
                }
            isProcessLoading.value = false
        }
    }

    fun killProcess(pid: Int) {
        viewModelScope.launch {
            service?.killProcess(pid)?.onSuccess { loadProcessList() }
        }
    }

    fun sendSignal(pid: Int, signal: Int) {
        viewModelScope.launch {
            service?.sendProcessSignal(pid, signal)?.onSuccess { loadProcessList() }
        }
    }

    fun loadMonitorHistory() {
        viewModelScope.launch {
            isHistoryLoading.value = true
            val now = System.currentTimeMillis()
            val start = now - 24 * 3600 * 1000L // 最近24小时
            service?.getMonitorHistory(start, now)
                ?.onSuccess { monitorHistory.value = it }
            isHistoryLoading.value = false
        }
    }

    fun showSetting() {
        viewModelScope.launch {
            service?.getMonitorSetting()?.onSuccess { monitorSetting.value = it }
        }
        showSettingDialog.value = true
    }

    fun hideSetting() { showSettingDialog.value = false }

    fun saveSetting(enabled: Boolean, days: Int, interval: Int) {
        viewModelScope.launch {
            service?.setMonitorSetting(MonitorSetting(enabled, days, interval))
                ?.onSuccess { monitorSetting.value = MonitorSetting(enabled, days, interval); hideSetting() }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            service?.clearMonitorData()?.onSuccess { monitorHistory.value = null }
        }
    }

    fun refresh() {
        viewModelScope.launch { fetchCurrentUsage() }
        loadTopProcesses()
    }

    override fun onCleared() {
        pollJob?.cancel()
        service?.close()
        super.onCleared()
    }
}
