package org.example.project.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.example.project.data.PanelConfig
import org.example.project.data.PanelRepository
import org.example.project.network.PanelApiService

class AddPanelViewModel : ViewModel() {

    // 表单字段
    val name = MutableStateFlow("")
    val scheme = MutableStateFlow("http")  // http 或 https
    val host = MutableStateFlow("")
    val port = MutableStateFlow("8888")
    val entrance = MutableStateFlow("")
    val authMode = MutableStateFlow("token")  // token 或 session
    val tokenId = MutableStateFlow("")
    val tokenSecret = MutableStateFlow("")
    val remark = MutableStateFlow("")

    // 测试连接状态
    private val _testStatus = MutableStateFlow<TestStatus>(TestStatus.Idle)
    val testStatus: StateFlow<TestStatus> = _testStatus

    // 保存状态
    private val _saveResult = MutableStateFlow<SaveResult>(SaveResult.Idle)
    val saveResult: StateFlow<SaveResult> = _saveResult

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
                // Session 模式有安全入口时先初始化入口（获取 verify_entrance Cookie）
                if (config.authMode == "session" && config.entrance.isNotEmpty()) {
                    service.initEntrance()
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
    fun savePanel(onTokenSaved: () -> Unit, onSessionSaved: (panelId: String) -> Unit) {
        val config = buildConfig() ?: run {
            _testStatus.value = TestStatus.Error(validateError())
            return
        }
        PanelRepository.addPanel(config)
        _saveResult.value = SaveResult.Success
        if (config.authMode == "session") {
            onSessionSaved(config.id)
        } else {
            onTokenSaved()
        }
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

    private fun buildConfig(): PanelConfig? {
        val h = host.value.trim()
        val p = port.value.trim().toIntOrNull() ?: return null
        val n = name.value.trim().ifEmpty { return null }
        if (h.isEmpty()) return null

        if (authMode.value == "token") {
            val tId = tokenId.value.trim().toLongOrNull() ?: return null
            val tSecret = tokenSecret.value.trim().ifEmpty { return null }
            return PanelConfig(
                id = generateId(),
                name = n,
                scheme = scheme.value,
                host = h,
                port = p,
                entrance = entrance.value.trim(),
                authMode = "token",
                tokenId = tId,
                tokenSecret = tSecret,
                remark = remark.value.trim()
            )
        } else {
            return PanelConfig(
                id = generateId(),
                name = n,
                scheme = scheme.value,
                host = h,
                port = p,
                entrance = entrance.value.trim(),
                authMode = "session",
                remark = remark.value.trim()
            )
        }
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
}
