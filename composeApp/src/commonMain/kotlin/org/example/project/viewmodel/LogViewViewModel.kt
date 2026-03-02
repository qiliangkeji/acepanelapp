package org.example.project.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.example.project.data.LogApiEntry
import org.example.project.data.PanelRepository
import org.example.project.data.SshLogApiItem
import org.example.project.network.PanelApiService

class LogViewViewModel : ViewModel() {

    // 0=面板日志(app), 1=数据库日志(db), 2=SSH审计
    val selectedTypeIndex = MutableStateFlow(0)

    val logs = MutableStateFlow<List<LogApiEntry>>(emptyList())
    val sshLogs = MutableStateFlow<List<SshLogApiItem>>(emptyList())
    val dates = MutableStateFlow<List<String>>(emptyList())
    val selectedDate = MutableStateFlow("")

    val isLoading = MutableStateFlow(false)
    val error = MutableStateFlow<String?>(null)

    private var service: PanelApiService? = null
    // 0=app 1=db 2=http 3=ssh
    private val typeKeys = listOf("app", "db", "http", "ssh")

    fun init(panelId: String) {
        val config = PanelRepository.getPanel(panelId) ?: return
        service?.close()
        service = PanelApiService(config)
        loadCurrentTab()
    }

    fun selectType(index: Int) {
        if (selectedTypeIndex.value == index) return
        selectedTypeIndex.value = index
        selectedDate.value = ""
        dates.value = emptyList()
        logs.value = emptyList()
        sshLogs.value = emptyList()
        loadCurrentTab()
    }

    fun selectDate(date: String) {
        selectedDate.value = date
        loadLogs()
    }

    fun loadCurrentTab() {
        val idx = selectedTypeIndex.value
        if (idx == 3) {
            loadSshLogs()
        } else {
            loadDates()
            loadLogs()
        }
    }

    private fun loadLogs() {
        viewModelScope.launch {
            isLoading.value = true
            error.value = null
            val type = typeKeys[selectedTypeIndex.value.coerceIn(0, 2)]
            service?.getLogs(type, limit = 100, date = selectedDate.value)
                ?.onSuccess { logs.value = it }
                ?.onFailure { error.value = it.message }
            isLoading.value = false
        }
    }

    private fun loadDates() {
        viewModelScope.launch {
            val type = typeKeys[selectedTypeIndex.value.coerceIn(0, 2)]
            service?.getLogDates(type)
                ?.onSuccess { dates.value = it }
        }
    }

    private fun loadSshLogs() {
        viewModelScope.launch {
            isLoading.value = true
            error.value = null
            service?.getSshLogs(100)
                ?.onSuccess { sshLogs.value = it }
                ?.onFailure { error.value = it.message }
            isLoading.value = false
        }
    }

    override fun onCleared() {
        service?.close()
        super.onCleared()
    }
}
