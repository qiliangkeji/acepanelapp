package org.example.project.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.example.project.data.PanelRepository
import org.example.project.network.PanelApiService
import org.example.project.network.SessionCookieStore

class TerminalViewModel : ViewModel() {

    enum class ConnState { IDLE, CONNECTING, CONNECTED, DISCONNECTED, ERROR }

    private val _lines = MutableStateFlow<List<String>>(emptyList())
    val lines: StateFlow<List<String>> = _lines

    private val _connState = MutableStateFlow(ConnState.IDLE)
    val connState: StateFlow<ConnState> = _connState

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private var service: PanelApiService? = null
    private var terminalJob: Job? = null
    private val inputChannel = Channel<String>(Channel.BUFFERED)
    private var sshId: Long = 0

    private val ansiRegex = Regex("\u001B\\[[0-9;?]*[A-Za-z]|\u001B[^\\[]")

    companion object {
        private const val MAX_LINES = 1000
    }

    fun init(panelId: String, sshId: Long = 0) {
        this.sshId = sshId
        val cfg = PanelRepository.getPanel(panelId) ?: run {
            _connState.value = ConnState.ERROR
            _errorMessage.value = "未找到面板配置"
            return
        }
        // WebSocket 只支持 Session Cookie 鉴权
        if (SessionCookieStore.get(panelId) == null) {
            _connState.value = ConnState.ERROR
            _errorMessage.value = "WebSocket 终端需要 Session 登录\n请先通过「登录」入口进行身份认证"
            return
        }
        service?.close()
        service = PanelApiService(cfg)
        connect()
    }

    fun connect() {
        terminalJob?.cancel()
        _connState.value = ConnState.CONNECTING
        _errorMessage.value = null
        val svc = service ?: return
        terminalJob = viewModelScope.launch {
            _connState.value = ConnState.CONNECTED
            val result = if (sshId > 0) {
                svc.startSshSession(sshId, inputChannel) { appendOutput(it) }
            } else {
                svc.startPtySession(inputChannel) { appendOutput(it) }
            }
            result.onSuccess {
                _connState.value = ConnState.DISCONNECTED
            }.onFailure { e ->
                _connState.value = ConnState.ERROR
                _errorMessage.value = e.message ?: "连接失败"
            }
        }
    }

    private fun appendOutput(raw: String) {
        val cleaned = ansiRegex.replace(raw, "")
            .replace("\r\n", "\n")
            .replace("\r", "")
        val newLines = cleaned.split("\n").dropLastWhile { it.isEmpty() }
        if (newLines.isEmpty()) return
        _lines.update { current ->
            val updated = current + newLines
            if (updated.size > MAX_LINES) updated.takeLast(MAX_LINES) else updated
        }
    }

    fun sendInput(text: String) {
        inputChannel.trySend(text)
    }

    fun sendLine(text: String) {
        inputChannel.trySend("$text\n")
    }

    fun sendCtrlC() {
        inputChannel.trySend("\u0003")
    }

    fun sendTab() {
        inputChannel.trySend("\t")
    }

    fun clear() {
        _lines.value = emptyList()
    }

    override fun onCleared() {
        terminalJob?.cancel()
        inputChannel.close()
        service?.close()
        super.onCleared()
    }
}
