package com.acepanel.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.acepanel.app.data.CreateWebsiteRequest
import com.acepanel.app.data.PanelRepository
import com.acepanel.app.network.PanelApiService

class AddWebsiteViewModel : ViewModel() {

    // 表单字段
    val websiteType = MutableStateFlow("php") // php / static / proxy
    val name = MutableStateFlow("")           // 网站标识名（^[a-zA-Z0-9_-]+$）
    val domains = MutableStateFlow("")        // 逗号分隔的域名列表
    val listenPort = MutableStateFlow("80")   // 监听端口
    val path = MutableStateFlow("")           // 运行目录
    val php = MutableStateFlow(0)             // PHP 版本（type=php 时必填）
    val proxy = MutableStateFlow("")          // 反代地址（type=proxy 时必填）
    val remark = MutableStateFlow("")

    // 联动建库
    val createDb = MutableStateFlow(false)
    val dbType = MutableStateFlow("mysql")
    val dbName = MutableStateFlow("")
    val dbUser = MutableStateFlow("")
    val dbPassword = MutableStateFlow("")

    // 从服务器加载的选项
    val installedPhp = MutableStateFlow<List<Int>>(emptyList())
    val webserver = MutableStateFlow("")

    val isLoadingEnv = MutableStateFlow(false)

    private val _submitStatus = MutableStateFlow<SubmitStatus>(SubmitStatus.Idle)
    val submitStatus: StateFlow<SubmitStatus> = _submitStatus

    private var service: PanelApiService? = null

    fun init(panelId: String) {
        val config = PanelRepository.getPanel(panelId) ?: return
        service?.close()
        service = PanelApiService(config)
        loadEnvironment()
    }

    private fun loadEnvironment() {
        viewModelScope.launch {
            isLoadingEnv.value = true
            service?.getInstalledEnvironment()?.onSuccess { env ->
                installedPhp.value = env.php.map { it.value }
                webserver.value = env.webserver
                if (env.php.isNotEmpty()) php.value = env.php.first().value
            }
            isLoadingEnv.value = false
        }
    }

    fun submit(onSuccess: () -> Unit) {
        val n = name.value.trim()
        if (n.isEmpty()) {
            _submitStatus.value = SubmitStatus.Error("网站名称不能为空")
            return
        }
        if (!n.matches(Regex("^[a-zA-Z0-9_-]+$"))) {
            _submitStatus.value = SubmitStatus.Error("网站名称只能包含字母、数字、下划线和连字符")
            return
        }
        val domainList = domains.value.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        if (domainList.isEmpty()) {
            _submitStatus.value = SubmitStatus.Error("至少填写一个域名")
            return
        }

        val req = CreateWebsiteRequest(
            type = websiteType.value,
            name = n,
            listens = listOf(listenPort.value.trim().ifEmpty { "80" }),
            domains = domainList,
            path = path.value.trim(),
            php = if (websiteType.value == "php") php.value else 0,
            proxy = if (websiteType.value == "proxy") proxy.value.trim() else "",
            db = createDb.value,
            db_type = if (createDb.value) dbType.value else "",
            db_name = if (createDb.value) dbName.value.trim() else "",
            db_user = if (createDb.value) dbUser.value.trim() else "",
            db_password = if (createDb.value) dbPassword.value else "",
            remark = remark.value.trim()
        )

        viewModelScope.launch {
            _submitStatus.value = SubmitStatus.Loading
            service?.createWebsite(req)
                ?.onSuccess {
                    _submitStatus.value = SubmitStatus.Success
                    onSuccess()
                }
                ?.onFailure { _submitStatus.value = SubmitStatus.Error(it.message ?: "未知错误") }
        }
    }

    sealed class SubmitStatus {
        object Idle : SubmitStatus()
        object Loading : SubmitStatus()
        object Success : SubmitStatus()
        data class Error(val message: String) : SubmitStatus()
    }

    override fun onCleared() {
        service?.close()
        super.onCleared()
    }
}
