package com.acepanel.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.acepanel.app.data.PanelConfig
import com.acepanel.app.data.PanelRepository
import com.acepanel.app.feedback.FeedbackCenter
import com.acepanel.app.network.PanelApiService

class AddPanelViewModel : ViewModel() {

    // 表单字段
    val name = MutableStateFlow("")
    val scheme = MutableStateFlow("http")  // http 或 https
    val host = MutableStateFlow("")
    val port = MutableStateFlow("8888")
    val entrance = MutableStateFlow("")
    val authMode = MutableStateFlow("session")  // token 或 session
    val quickPaste = MutableStateFlow("")
    val sessionUsername = MutableStateFlow("")
    val sessionPassword = MutableStateFlow("")
    val userAgent = MutableStateFlow("")
    val tokenId = MutableStateFlow("")
    val tokenSecret = MutableStateFlow("")
    val remark = MutableStateFlow("")

    private var editingPanelId: String? = null
    private var lastAutoImportKey: String = ""

    // 测试连接状态
    private val _testStatus = MutableStateFlow<TestStatus>(TestStatus.Idle)
    val testStatus: StateFlow<TestStatus> = _testStatus

    // 保存状态
    private val _saveResult = MutableStateFlow<SaveResult>(SaveResult.Idle)
    val saveResult: StateFlow<SaveResult> = _saveResult

    fun init(panelId: String? = null) {
        lastAutoImportKey = ""
        _testStatus.value = TestStatus.Idle
        _saveResult.value = SaveResult.Idle

        if (panelId.isNullOrBlank()) {
            editingPanelId = null
            resetForm()
            return
        }

        val config = PanelRepository.getPanel(panelId)
        if (config == null) {
            editingPanelId = null
            resetForm()
            FeedbackCenter.error("面板不存在", "无法加载要编辑的面板配置")
            return
        }

        editingPanelId = config.id
        quickPaste.value = ""
        name.value = config.name
        scheme.value = config.scheme
        host.value = config.host
        port.value = config.port.toString()
        entrance.value = config.entrance
        authMode.value = config.authMode
        sessionUsername.value = config.sessionUsername
        sessionPassword.value = config.sessionPassword
        userAgent.value = config.userAgent
        tokenId.value = if (config.tokenId > 0) config.tokenId.toString() else ""
        tokenSecret.value = config.tokenSecret
        remark.value = config.remark
    }

    /** 测试连接 */
    fun testConnection() {
        val config = buildConfig() ?: run {
            _testStatus.value = TestStatus.Error(validateError())
            return
        }
        viewModelScope.launch {
            _testStatus.value = TestStatus.Testing
            try {
                val service = PanelApiService(config)
                // Session 模式先初始化入口（获取 verify_entrance Cookie）；入口为 / 时也需要建立同一个 session
                if (config.authMode == "session") {
                    service.initEntrance().onFailure { e ->
                        service.close()
                        _testStatus.value = TestStatus.Error("连接失败: ${e.message}")
                        return@launch
                    }
                }
                if (config.authMode == "token") {
                    val authResult = service.getCurrentUsage()
                    service.close()
                    if (authResult.isSuccess) {
                        val usage = authResult.getOrNull()
                        val host = usage?.host?.hostname?.takeIf { it.isNotBlank() }
                        val suffix = host?.let { "（主机: $it）" } ?: ""
                        _testStatus.value = TestStatus.Success("连接并鉴权成功！$suffix")
                    } else {
                        val err = authResult.exceptionOrNull()?.message ?: "未知错误"
                        _testStatus.value = TestStatus.Error("连接失败: $err")
                    }
                } else {
                    val panelResult = service.getPanelInfo()
                    service.close()
                    if (panelResult.isSuccess) {
                        val info = panelResult.getOrNull()
                        _testStatus.value = TestStatus.Success("连接成功！面板: ${info?.name ?: "未知"}")
                    } else {
                        val err = panelResult.exceptionOrNull()?.message ?: "未知错误"
                        _testStatus.value = TestStatus.Error("连接失败: $err")
                    }
                }
            } catch (e: Exception) {
                _testStatus.value = TestStatus.Error("连接失败: ${e.message}")
            }
        }
    }

    /**
     * 保存面板配置。
     * Token 模式 -> 调 onTokenSaved 直接返回
     * Session 模式 -> 调 onSessionSaved(panelId) 跳转到登录页
     */
    fun savePanel(onTokenSaved: () -> Unit, onSessionSaved: (panelId: String, username: String, password: String) -> Unit) {
        val config = buildConfig() ?: run {
            val message = validateError()
            _testStatus.value = TestStatus.Error(message)
            FeedbackCenter.warning("无法保存面板", message)
            return
        }
        saveConfig(config)
        _saveResult.value = SaveResult.Success
        FeedbackCenter.success(if (isEditing()) "面板已更新" else "面板已保存", config.name)
        if (config.authMode == "session") {
            onSessionSaved(config.id, config.sessionUsername, config.sessionPassword)
        } else {
            onTokenSaved()
        }
    }

    fun onQuickPasteChanged(
        value: String,
        onAutoLogin: (panelId: String, username: String, password: String) -> Unit
    ) {
        quickPaste.value = value
        val parsed = parsePastedPanelInfo(value)
        applyParsedInfo(parsed)

        if (!parsed.isReadyForAutoLogin) return

        val importKey = listOf(
            parsed.username,
            parsed.password,
            parsed.scheme,
            parsed.host,
            parsed.port,
            parsed.entrance,
            parsed.userAgent
        ).joinToString("\n")
        if (importKey == lastAutoImportKey) return
        lastAutoImportKey = importKey

        val config = buildConfig(
            sessionUsernameOverride = parsed.username,
            sessionPasswordOverride = parsed.password
        ) ?: run {
            _testStatus.value = TestStatus.Error(validateError())
            return
        }
        saveConfig(config)
        _saveResult.value = SaveResult.Success
        _testStatus.value = TestStatus.Success("已识别面板信息，正在自动登录")
        FeedbackCenter.success(if (isEditing()) "面板信息已更新" else "面板信息已导入", config.name)
        onAutoLogin(config.id, parsed.username, parsed.password)
    }

    /** 返回具体的验证错误信息，帮助用户知道填什么 */
    private fun validateError(): String {
        if (name.value.trim().isEmpty()) return "请填写面板名称"
        if (host.value.trim().isEmpty()) return "请填写主机地址（IP 或域名）"
        if (port.value.trim().toIntOrNull() == null) return "端口必须是数字（默认 8888）"
        if (authMode.value == "token") {
            val tidStr = tokenId.value.trim()
            if (tidStr.isEmpty()) return "Token 模式需要填写 Token ID\n提示：Token ID 是面板「设置→API密钥」中显示的数字编号，不是用户名"
            if (tidStr.toLongOrNull() == null) return "Token ID 必须是纯数字（例如：1、2、3……）\n您填写的「$tidStr」不是有效数字"
            if (tokenSecret.value.trim().isEmpty()) return "Token 模式需要填写 Token Secret\n提示：Token Secret 是创建 API密钥时面板显示的密钥字符串，不是登录密码"
        }
        return "请检查填写的信息是否完整"
    }

    private fun buildConfig(
        sessionUsernameOverride: String = sessionUsername.value,
        sessionPasswordOverride: String = sessionPassword.value
    ): PanelConfig? {
        val h = host.value.trim()
        val p = port.value.trim().toIntOrNull() ?: return null
        val n = name.value.trim().ifEmpty { return null }
        if (h.isEmpty()) return null

        if (authMode.value == "token") {
            val tId = tokenId.value.trim().toLongOrNull() ?: return null
            val tSecret = tokenSecret.value.trim().ifEmpty { return null }
            return PanelConfig(
                id = editingPanelId ?: generateId(),
                name = n,
                scheme = scheme.value,
                host = h,
                port = p,
                entrance = entrance.value.trim(),
                authMode = "token",
                tokenId = tId,
                tokenSecret = tSecret,
                userAgent = userAgent.value.trim(),
                remark = remark.value.trim()
            )
        } else {
            val existingPassword = editingPanelId?.let { PanelRepository.getPanel(it)?.sessionPassword }.orEmpty()
            val savedPassword = sessionPasswordOverride.ifBlank { existingPassword }
            return PanelConfig(
                id = editingPanelId ?: generateId(),
                name = n,
                scheme = scheme.value,
                host = h,
                port = p,
                entrance = entrance.value.trim(),
                authMode = "session",
                sessionUsername = sessionUsernameOverride.trim(),
                sessionPassword = savedPassword,
                userAgent = userAgent.value.trim(),
                remark = remark.value.trim()
            )
        }
    }

    private fun resetForm() {
        name.value = ""
        scheme.value = "http"
        host.value = ""
        port.value = "8888"
        entrance.value = ""
        authMode.value = "session"
        quickPaste.value = ""
        sessionUsername.value = ""
        sessionPassword.value = ""
        userAgent.value = ""
        tokenId.value = ""
        tokenSecret.value = ""
        remark.value = ""
    }

    private fun saveConfig(config: PanelConfig) {
        if (isEditing() && PanelRepository.getPanel(config.id) != null) {
            PanelRepository.updatePanel(config)
        } else {
            PanelRepository.addPanel(config)
        }
    }

    private fun isEditing(): Boolean = editingPanelId != null

    private fun applyParsedInfo(parsed: ParsedPanelInfo) {
        if (parsed.scheme.isNotBlank()) scheme.value = parsed.scheme
        if (parsed.host.isNotBlank()) host.value = parsed.host
        if (parsed.port.isNotBlank()) port.value = parsed.port
        if (parsed.entrance.isNotBlank()) entrance.value = parsed.entrance
        if (parsed.username.isNotBlank()) sessionUsername.value = parsed.username
        if (parsed.password.isNotBlank()) sessionPassword.value = parsed.password
        if (parsed.userAgent.isNotBlank()) userAgent.value = parsed.userAgent
        if (parsed.host.isNotBlank() && name.value.isBlank()) {
            name.value = parsed.host
        }
        if (parsed.username.isNotBlank() || parsed.password.isNotBlank()) {
            authMode.value = "session"
        }
    }

    private fun parsePastedPanelInfo(raw: String): ParsedPanelInfo {
        if (raw.isBlank()) return ParsedPanelInfo()

        var parsed = ParsedPanelInfo()
        var publicUrl: ParsedUrl? = null
        var localUrl: ParsedUrl? = null
        var firstUrl: ParsedUrl? = null

        raw.lines().forEach { rawLine ->
            val line = rawLine.trim().trim('│', '|').trim()
            if (line.isBlank()) return@forEach
            val normalized = line.replace('：', ':')
            val label = normalized.substringBefore(":", "")
            val value = normalized.substringAfter(":", "").takeIf { it != normalized }
                ?.trim()
                ?.trim('│', '|')
                ?.trim()
                .orEmpty()

            when {
                label.contains("用户名") || label.contains("账号") ->
                    if (value.isNotPlaceholder()) parsed = parsed.copy(username = value)
                label.contains("密码") ->
                    if (value.isNotPlaceholder()) parsed = parsed.copy(password = value)
                label.contains("端口") ->
                    value.toIntOrNull()?.let { parsed = parsed.copy(port = it.toString()) }
                label.contains("入口") ->
                    normalizeEntrance(value).takeIf { it.isNotPlaceholder() }?.let {
                        parsed = parsed.copy(entrance = it)
                    }
                label.equals("ua", ignoreCase = true) ||
                    label.equals("user-agent", ignoreCase = true) ||
                    label.contains("浏览器标识") ||
                    label.contains("用户代理") ->
                    if (value.isNotPlaceholder()) parsed = parsed.copy(userAgent = value)
                label.contains("主机") || label.contains("地址") ->
                    if (value.isNotPlaceholder() && !value.contains("://")) parsed = parsed.copy(host = value)
            }

            val url = parseUrlFromLine(line)
            if (url != null) {
                firstUrl = firstUrl ?: url
                if (normalized.contains("公网")) publicUrl = url
                if (normalized.contains("本地")) localUrl = url
            }
        }

        val selectedUrl = publicUrl ?: localUrl ?: firstUrl
        if (selectedUrl != null) {
            parsed = parsed.copy(
                scheme = selectedUrl.scheme.ifBlank { parsed.scheme },
                host = selectedUrl.host.ifBlank { parsed.host },
                port = parsed.port.ifBlank { selectedUrl.port },
                entrance = parsed.entrance.ifBlank { selectedUrl.entrance }
            )
        }

        return parsed
    }

    private fun parseUrlFromLine(line: String): ParsedUrl? {
        val urlText = Regex("""https?://[^\s│|]+""", RegexOption.IGNORE_CASE)
            .find(line)
            ?.value
            ?: return null
        val match = Regex("""^(https?)://([^/:/?#\s]+)(?::([^/?#\s]+))?([^?#\s]*)""", RegexOption.IGNORE_CASE)
            .find(urlText)
            ?: return null
        val scheme = match.groupValues.getOrNull(1).orEmpty().lowercase()
        val host = match.groupValues.getOrNull(2).orEmpty()
        val port = match.groupValues.getOrNull(3).orEmpty().takeIf { it.toIntOrNull() != null }.orEmpty()
        val entrance = normalizeEntrance(match.groupValues.getOrNull(4).orEmpty())
        return ParsedUrl(scheme = scheme, host = host, port = port, entrance = entrance)
    }

    private fun normalizeEntrance(value: String): String {
        val raw = value
            .substringBefore("?")
            .substringBefore("#")
            .trim()
            .trim('│', '|', '\'', '"', '`', '，', ',', '。', '.', '；', ';')
            .trim()
        if (!raw.isNotPlaceholder()) return ""
        val path = if (raw.contains("://")) {
            Regex("""https?://[^\s│|]+""", RegexOption.IGNORE_CASE)
                .find(raw)
                ?.value
                ?.let { parseUrlFromLine(it)?.entrance }
                .orEmpty()
        } else {
            raw.trim('/')
        }
        return path.substringBefore("/").trim()
    }

    private fun String.isNotPlaceholder(): Boolean {
        val value = trim().trim('│', '|').trim()
        if (value.isBlank()) return false
        val lower = value.lowercase()
        return lower !in setOf("xxxxx", "xxxx", "xxx", "端口", "入口", "port", "entrance")
    }

    private fun generateId(): String {
        // 生成随机 ID（KMP 兼容）
        val chars = "abcdefghijklmnopqrstuvwxyz0123456789"
        return buildString {
            repeat(8) { append(chars.random()) }
            append('-')
            repeat(4) { append(chars.random()) }
            append('-')
            repeat(4) { append(chars.random()) }
            append('-')
            repeat(12) { append(chars.random()) }
        }
    }

    sealed class TestStatus {
        object Idle : TestStatus()
        object Testing : TestStatus()
        data class Success(val message: String) : TestStatus()
        data class Error(val message: String) : TestStatus()
    }

    sealed class SaveResult {
        object Idle : SaveResult()
        object Success : SaveResult()
        data class Error(val message: String) : SaveResult()
    }

    private data class ParsedPanelInfo(
        val username: String = "",
        val password: String = "",
        val scheme: String = "",
        val host: String = "",
        val port: String = "",
        val entrance: String = "",
        val userAgent: String = ""
    ) {
        val isReadyForAutoLogin: Boolean
            get() = username.isNotBlank() &&
                password.isNotBlank() &&
                host.isNotBlank() &&
                port.toIntOrNull() != null
    }

    private data class ParsedUrl(
        val scheme: String,
        val host: String,
        val port: String,
        val entrance: String
    )
}
