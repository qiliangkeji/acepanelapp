package org.example.project.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.example.project.data.AppCategoryItem
import org.example.project.data.AppDetailItem
import org.example.project.data.PanelRepository
import org.example.project.network.PanelApiService

class SoftwareStoreViewModel : ViewModel() {

    val categories = MutableStateFlow<List<AppCategoryItem>>(emptyList())
    val apps = MutableStateFlow<List<AppDetailItem>>(emptyList())
    val total = MutableStateFlow(0)
    val selectedCategoryValue = MutableStateFlow("")
    val searchQuery = MutableStateFlow("")

    val isLoading = MutableStateFlow(false)
    val error = MutableStateFlow<String?>(null)

    // 安装/卸载操作状态 slug -> true 表示操作中
    val operatingSlug = MutableStateFlow<String?>(null)
    val operationError = MutableStateFlow<String?>(null)

    private var service: PanelApiService? = null

    fun init(panelId: String) {
        val config = PanelRepository.getPanel(panelId) ?: return
        service?.close()
        service = PanelApiService(config)
        loadCategories()
        loadApps()
    }

    fun loadCategories() {
        viewModelScope.launch {
            service?.getAppCategories()
                ?.onSuccess { categories.value = it }
        }
    }

    fun loadApps(category: String = selectedCategoryValue.value) {
        selectedCategoryValue.value = category
        viewModelScope.launch {
            isLoading.value = true
            error.value = null
            service?.getApps(page = 1, limit = 50, category = category)
                ?.onSuccess {
                    apps.value = it.items
                    total.value = it.total
                }
                ?.onFailure { error.value = it.message }
            isLoading.value = false
        }
    }

    fun installApp(slug: String, channel: String = "stable") {
        viewModelScope.launch {
            operatingSlug.value = slug
            operationError.value = null
            service?.installApp(slug, channel)
                ?.onSuccess { loadApps() }
                ?.onFailure { operationError.value = it.message }
            operatingSlug.value = null
        }
    }

    fun uninstallApp(slug: String) {
        viewModelScope.launch {
            operatingSlug.value = slug
            operationError.value = null
            service?.uninstallApp(slug)
                ?.onSuccess { loadApps() }
                ?.onFailure { operationError.value = it.message }
            operatingSlug.value = null
        }
    }

    // 本地搜索过滤
    val filteredApps get() = apps.value.let { list ->
        val q = searchQuery.value.trim().lowercase()
        if (q.isEmpty()) list
        else list.filter {
            it.name.lowercase().contains(q) ||
                    it.slug.lowercase().contains(q) ||
                    it.description.lowercase().contains(q)
        }
    }

    override fun onCleared() {
        service?.close()
        super.onCleared()
    }
}
