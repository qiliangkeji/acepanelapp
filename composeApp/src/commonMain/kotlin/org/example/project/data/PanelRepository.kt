package org.example.project.data

import com.russhwolf.settings.Settings
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * 面板配置本地仓库，单例模式
 * 使用 multiplatform-settings-no-arg 跨平台持久化存储
 * - Android: SharedPreferences
 * - iOS: NSUserDefaults
 */
object PanelRepository {

    private const val KEY_PANELS = "panel_configs_v1"
    private const val KEY_SESSION_PREFIX = "session_cookie_"

    // lazy 确保在平台初始化完成后再访问
    private val settings: Settings by lazy { Settings() }

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
        encodeDefaults = true
    }

    // 内存缓存，避免反复反序列化
    private var cachedPanels: MutableList<PanelConfig>? = null

    fun getPanels(): List<PanelConfig> {
        if (cachedPanels == null) {
            cachedPanels = loadFromStorage().toMutableList()
        }
        return cachedPanels!!.toList()
    }

    fun getPanel(id: String): PanelConfig? = getPanels().find { it.id == id }

    fun addPanel(config: PanelConfig) {
        val list = ensureCache()
        list.removeAll { it.id == config.id }
        list.add(config)
        persist(list)
    }

    fun updatePanel(config: PanelConfig) {
        val list = ensureCache()
        val idx = list.indexOfFirst { it.id == config.id }
        if (idx >= 0) {
            list[idx] = config
            persist(list)
        }
    }

    fun removePanel(id: String) {
        val list = ensureCache()
        list.removeAll { it.id == id }
        persist(list)
        settings.remove("$KEY_SESSION_PREFIX$id")
    }

    /** 保存 Session Cookie（用于 WebSocket 鉴权） */
    fun saveSessionCookie(panelId: String, cookie: String) {
        settings.putString("$KEY_SESSION_PREFIX$panelId", cookie)
    }

    fun getSessionCookie(panelId: String): String? {
        val v = settings.getStringOrNull("$KEY_SESSION_PREFIX$panelId")
        return if (v.isNullOrEmpty()) null else v
    }

    private fun ensureCache(): MutableList<PanelConfig> {
        if (cachedPanels == null) {
            cachedPanels = loadFromStorage().toMutableList()
        }
        return cachedPanels!!
    }

    private fun loadFromStorage(): List<PanelConfig> {
        val raw = settings.getStringOrNull(KEY_PANELS) ?: return emptyList()
        if (raw.isEmpty()) return emptyList()
        return try {
            json.decodeFromString<List<PanelConfig>>(raw)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun persist(list: List<PanelConfig>) {
        settings.putString(KEY_PANELS, json.encodeToString(list))
    }
}
