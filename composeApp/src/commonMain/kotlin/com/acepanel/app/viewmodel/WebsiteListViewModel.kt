package com.acepanel.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.acepanel.app.data.PanelRepository
import com.acepanel.app.data.WebsiteApiItem
import com.acepanel.app.network.PanelApiService

class WebsiteListViewModel : ViewModel() {

    private val _websites = MutableStateFlow<List<WebsiteApiItem>>(emptyList())
    val websites: StateFlow<List<WebsiteApiItem>> = _websites

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _total = MutableStateFlow(0)
    val total: StateFlow<Int> = _total

    val searchQuery = MutableStateFlow("")

    private var service: PanelApiService? = null
    private var panelId: String = ""
    private var loadJob: Job? = null

    fun init(pId: String) {
        panelId = pId
        val cfg = PanelRepository.getPanel(pId) ?: return
        service?.close()
        service = PanelApiService(cfg)
        loadWebsites()
    }

    fun loadWebsites() {
        val svc = service ?: return
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                svc.getWebsites(type = "all", page = 1, limit = 100)
                    .onSuccess { resp ->
                        _websites.value = resp.items
                        _total.value = resp.total
                    }
                    .onFailure { _error.value = it.message }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    /** 切换网站启停 */
    fun toggleStatus(item: WebsiteApiItem) {
        val svc = service ?: return
        viewModelScope.launch {
            val newStatus = !item.status
            svc.setWebsiteStatus(item.id, newStatus).onSuccess {
                _websites.value = _websites.value.map {
                    if (it.id == item.id) it.copy(status = newStatus) else it
                }
            }.onFailure { _error.value = "切换状态失败: ${it.message}" }
        }
    }

    /** 删除网站 */
    fun deleteWebsite(id: Long, deletePath: Boolean = false) {
        val svc = service ?: return
        viewModelScope.launch {
            svc.deleteWebsite(id, deletePath, false).onSuccess {
                _websites.value = _websites.value.filter { it.id != id }
                _total.value = _total.value - 1
            }.onFailure { _error.value = "删除失败: ${it.message}" }
        }
    }

    /** 本地搜索过滤 */
    fun filteredWebsites(): List<WebsiteApiItem> {
        val q = searchQuery.value.trim().lowercase()
        if (q.isEmpty()) return _websites.value
        return _websites.value.filter { w ->
            w.name.lowercase().contains(q) || w.domains.any { d -> d.lowercase().contains(q) }
        }
    }

    override fun onCleared() {
        loadJob?.cancel()
        service?.close()
        service = null
        super.onCleared()
    }
}
