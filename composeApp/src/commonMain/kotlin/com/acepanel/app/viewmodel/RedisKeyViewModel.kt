package com.acepanel.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import com.acepanel.app.data.CreateRedisKeyRequest
import com.acepanel.app.data.DatabaseServerApiItem
import com.acepanel.app.data.PanelRepository
import com.acepanel.app.data.RedisKVItem
import com.acepanel.app.network.PanelApiService

class RedisKeyViewModel : ViewModel() {

    // Redis 服务器列表（仅 redis 类型）
    private val _redisServers = MutableStateFlow<List<DatabaseServerApiItem>>(emptyList())
    val redisServers: StateFlow<List<DatabaseServerApiItem>> = _redisServers

    private val _selectedServerId = MutableStateFlow<Long?>(null)
    val selectedServerId: StateFlow<Long?> = _selectedServerId

    // DB 选择
    private val _dbCount = MutableStateFlow(1)
    val dbCount: StateFlow<Int> = _dbCount

    private val _selectedDb = MutableStateFlow(0)
    val selectedDb: StateFlow<Int> = _selectedDb

    // 键列表
    private val _keys = MutableStateFlow<List<RedisKVItem>>(emptyList())
    val keys: StateFlow<List<RedisKVItem>> = _keys

    private val _searchText = MutableStateFlow("")
    val searchText: StateFlow<String> = _searchText

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _actionKey = MutableStateFlow<String?>(null)
    val actionKey: StateFlow<String?> = _actionKey

    private val _actionError = MutableStateFlow<String?>(null)
    val actionError: StateFlow<String?> = _actionError

    // 键详情弹窗
    private val _detailKey = MutableStateFlow<RedisKVItem?>(null)
    val detailKey: StateFlow<RedisKVItem?> = _detailKey

    private val _isDetailLoading = MutableStateFlow(false)
    val isDetailLoading: StateFlow<Boolean> = _isDetailLoading

    private var service: PanelApiService? = null

    fun init(panelId: String) {
        val cfg = PanelRepository.getPanel(panelId) ?: return
        service?.close()
        service = PanelApiService(cfg)
        viewModelScope.launch { loadServers() }
    }

    private suspend fun loadServers() {
        service?.getDatabaseServers(limit = 50)
            ?.onSuccess { resp ->
                val redis = resp.items.filter { it.type.lowercase() == "redis" }
                _redisServers.value = redis
                if (redis.isNotEmpty() && _selectedServerId.value == null) {
                    selectServer(redis.first().id)
                }
            }
            ?.onFailure { _error.value = it.message }
    }

    fun selectServer(serverId: Long) {
        _selectedServerId.value = serverId
        _selectedDb.value = 0
        _keys.value = emptyList()
        viewModelScope.launch { loadDbCount(serverId) }
    }

    private suspend fun loadDbCount(serverId: Long) {
        service?.getRedisDatabases(serverId)
            ?.onSuccess { count ->
                _dbCount.value = if (count > 0) count else 1
                load()
            }
            ?.onFailure {
                _dbCount.value = 1
                load()
            }
    }

    fun selectDb(db: Int) {
        _selectedDb.value = db
        viewModelScope.launch { load() }
    }

    fun setSearch(text: String) {
        _searchText.value = text
    }

    fun search() {
        viewModelScope.launch { load() }
    }

    fun load() {
        val serverId = _selectedServerId.value ?: return
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            val pattern = _searchText.value.trim().let { if (it.isNotEmpty()) "*$it*" else "" }
            service?.getRedisData(serverId, _selectedDb.value, pattern, limit = 100)
                ?.onSuccess { _keys.value = it.items }
                ?.onFailure { _error.value = it.message }
            _isLoading.value = false
        }
    }

    fun showKeyDetail(item: RedisKVItem) {
        val serverId = _selectedServerId.value ?: return
        viewModelScope.launch {
            _isDetailLoading.value = true
            _detailKey.value = item
            service?.getRedisKey(serverId, item.key, _selectedDb.value)
                ?.onSuccess { _detailKey.value = it }
                ?.onFailure { /* 保持摘要数据 */ }
            _isDetailLoading.value = false
        }
    }

    fun hideKeyDetail() {
        _detailKey.value = null
    }

    fun deleteKey(key: String) {
        val serverId = _selectedServerId.value ?: return
        viewModelScope.launch {
            _actionKey.value = key
            _actionError.value = null
            service?.deleteRedisKey(serverId, _selectedDb.value, key)
                ?.onSuccess { _keys.value = _keys.value.filter { it.key != key } }
                ?.onFailure { _actionError.value = "删除失败: ${it.message}" }
            _actionKey.value = null
        }
    }

    private val _showCreateDialog = MutableStateFlow(false)
    val showCreateDialog: StateFlow<Boolean> = _showCreateDialog

    private val _isCreating = MutableStateFlow(false)
    val isCreating: StateFlow<Boolean> = _isCreating

    fun showCreate() { _showCreateDialog.value = true; _actionError.value = null }
    fun hideCreate() { _showCreateDialog.value = false }

    fun createKey(key: String, value: String, type: String, ttl: Long) {
        val serverId = _selectedServerId.value ?: return
        val validationError = validateRedisValue(type, value)
        if (validationError != null) {
            _actionError.value = validationError
            return
        }
        viewModelScope.launch {
            _isCreating.value = true
            _actionError.value = null
            service?.setRedisKey(
                CreateRedisKeyRequest(serverId, _selectedDb.value, key, value, type, ttl)
            )?.onSuccess {
                _showCreateDialog.value = false
                load()
            }?.onFailure { _actionError.value = "创建失败: ${it.message}" }
            _isCreating.value = false
        }
    }

    private fun validateRedisValue(type: String, value: String): String? {
        if (value.isBlank()) return "值不能为空"
        if (type == "string") return null
        val element = runCatching { Json.parseToJsonElement(value) }.getOrNull()
            ?: return "非 String 类型的值必须是合法 JSON"
        return when (type) {
            "list", "set" -> if (element is JsonArray) null else "List/Set 类型的值必须是 JSON 数组，例如 [\"a\",\"b\"]"
            "hash", "zset" -> if (element is JsonObject) null else "Hash/ZSet 类型的值必须是 JSON 对象，例如 {\"field\":\"value\"}"
            else -> null
        }
    }

    fun updateKeyTTL(key: String, ttl: Long) {
        val serverId = _selectedServerId.value ?: return
        viewModelScope.launch {
            _actionError.value = null
            service?.setRedisKeyTTL(serverId, _selectedDb.value, key, ttl)
                ?.onFailure { _actionError.value = "TTL更新失败: ${it.message}" }
        }
    }

    fun clearDb() {
        val serverId = _selectedServerId.value ?: return
        viewModelScope.launch {
            _actionError.value = null
            service?.clearRedisDB(serverId, _selectedDb.value)
                ?.onSuccess { _keys.value = emptyList() }
                ?.onFailure { _actionError.value = "清空失败: ${it.message}" }
        }
    }

    override fun onCleared() {
        service?.close()
        super.onCleared()
    }
}
